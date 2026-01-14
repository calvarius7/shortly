### Automatisches Setup (Empfohlen)

Für ein professionelles Management des Clusters mit State-Verwaltung und Idempotenz:

```bash
cd deploy/tofu
# terraform.tfvars ausfüllen (siehe terraform.tfvars.example)
tofu init
tofu apply
```

Details findest du in der [README-TOFU.md](./tofu/README-TOFU.md).

### Wie deployen (Manuell)

1) Image bauen und pushen

```
docker build -t YOUR_REGISTRY/shortly:0.0.1 .
docker push YOUR_REGISTRY/shortly:0.0.1
```

2) Prometheus+Grafana (kube-prometheus-stack) installieren

```
kubectl create namespace monitoring
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm upgrade --install kube-prometheus-stack \
  prometheus-community/kube-prometheus-stack \
  --namespace monitoring
```

Hinweis: Der mitgelieferte `ServiceMonitor` ist standardmäßig mit `labels.release=kube-prometheus-stack` versehen, damit
Prometheus Operator ihn automatisch aufnimmt. Falls Ihr Release-Name anders ist, bitte in `values.yaml` anpassen.

3) Loki + Promtail installieren (Logs)

```
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update
helm upgrade --install loki grafana/loki --namespace monitoring --create-namespace \
  --set service.type=ClusterIP
helm upgrade --install promtail grafana/promtail --namespace monitoring \
  --set config.clients[0].url=http://loki.monitoring.svc.cluster.local:3100/loki/api/v1/push
```

4) Shortly + Redis installieren (Helm-Chart in diesem Repo)

```
kubectl create namespace shortly
helm upgrade --install shortly ./deploy/helm/shortly \
  --namespace shortly \
  --set image.repository=YOUR_REGISTRY/shortly \
  --set image.tag=0.0.1
```

- Service-Typ per Value änderbar (z. B. `--set service.type=LoadBalancer`).
- Ingress aktivieren:

```
helm upgrade --install shortly ./deploy/helm/shortly \
  -n shortly \
  --set ingress.enabled=true \
  --set ingress.className=nginx \
  --set ingress.hosts[0].host=shortly.local \
  --set ingress.hosts[0].paths[0].path=/ \
  --set ingress.hosts[0].paths[0].pathType=Prefix
```

5) Verifizieren

```
kubectl get pods -n shortly
kubectl -n shortly port-forward svc/shortly 8080:8080
curl -f http://localhost:8080/actuator/health
curl -f http://localhost:8080/actuator/prometheus | head
```

### Was das Chart bereitstellt

- App-Deployment mit Readiness-/Liveness-Probes (Actuator) und Ressourcen-Settings (per Values)
- Service mit optionalen Prometheus-Scrape-Annotations
- Optionaler `ServiceMonitor` (für Prometheus Operator)
- Separater Redis-Deployment/Service (+ optional PVC via Values)
- Optionale Ingress-Ressource

### Wichtige Values (siehe `deploy/helm/shortly/values.yaml`)

- `image.repository`, `image.tag` (auf euer Registry-Image setzen)
- `replicaCount`
- `service.type` (ClusterIP/NodePort/LoadBalancer)
- `ingress.enabled`, `ingress.className`, `ingress.hosts`
- `redis.enabled`, `redis.persistence.enabled`, `redis.persistence.size`
- `monitoring.annotationsScrape` (true = Service-Annotations für Prometheus)
- `monitoring.serviceMonitor.enabled` (CRD durch kube-prometheus-stack nötig)

### Hinweise

- Die App exportiert Metriken unter `/actuator/prometheus`. Health-Probes unter `/actuator/health/readiness` und
  `/actuator/health/liveness` sind aktiviert.
- Redis läuft in einem eigenen Pod/Service. Für Produktion ggf. `bitnami/redis` (HA/Persistence) oder Managed Redis
  einsetzen.
- Falls euer Prometheus-Operator-Release nicht `kube-prometheus-stack` heißt, bitte
  `values.monitoring.serviceMonitor.additionalLabels.release` anpassen.
- Falls ein privates Registry-Secret benötigt wird: `imagePullSecrets` in `values.yaml` setzen.

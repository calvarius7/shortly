# ArgoCD Deployment Fixes

## Probleme die behoben wurden

### 1. **Alte Template-Dateien gelöscht**
- `deployment.yaml` und `service.yaml` wurden gelöscht
- Diese versuchten ein `shortly:latest` Image zu deployen, das nicht existiert
- Jetzt werden nur noch `frontend.yaml`, `backend.yaml`, und `redis-deployment.yaml` verwendet

### 2. **Image Registry-Pfade korrigiert**

#### In `values.yaml`:
- Frontend: `projectregistry.neusta.de/limmoor/shortly/frontend:latest`
- Backend: `projectregistry.neusta.de/limmoor/shortly/java:latest`  
- Redis: `docker.io/redis:8-alpine` (explizite Docker Hub Registry)

#### In `values-docker-desktop.yaml`:
- Gleiche Registry-Pfade + `imagePullSecrets: gitlab-registry`
- Redis: `docker.io/redis:8-alpine`

### 3. **Redis Hostname korrigiert**
- Backend-Umgebungsvariable `SPRING_DATA_REDIS_HOST` auf `shortly-shortly-redis` gesetzt
- Der Service heißt `{{ release }}-{{ chart }}-redis`, also bei Release "shortly": `shortly-shortly-redis`

### 4. **Frontend Health-Checks korrigiert**
- Liveness/Readiness Probes von `/health` auf `/` geändert
- NGINX exposed keinen `/health` Endpoint, aber `/` funktioniert

### 5. **Ingress standardmäßig disabled**
- In `values.yaml`: `ingress.enabled: false`
- Wird in `values-docker-desktop.yaml` auf `true` überschrieben

### 6. **Prometheus Monitoring für Backend aktiviert**
- Backend Service hat jetzt Prometheus-Annotations:
  - `prometheus.io/scrape: "true"`
  - `prometheus.io/path: "/actuator/prometheus"`
  - `prometheus.io/port: "8080"`

## Deployment-Reihenfolge

1. **GitLab Registry Secret erstellen** (einmalig):
```bash
kubectl create namespace shortly
kubectl create secret docker-registry gitlab-registry \
  --docker-server=projectregistry.neusta.de \
  --docker-username=<USERNAME> \
  --docker-password=<TOKEN> \
  --docker-email=<EMAIL> \
  -n shortly
```

2. **ArgoCD Application deployen**:
```bash
kubectl apply -f deploy/argocd/shortly-project.yaml
kubectl apply -f deploy/argocd/shortly-app.yaml
```

3. **Sync Status prüfen**:
```bash
kubectl get pods -n shortly
# Erwartet:
# - shortly-shortly-frontend-xxx
# - shortly-shortly-backend-xxx
# - shortly-shortly-redis-xxx
```

## Verifikation

### Pods prüfen:
```bash
kubectl get pods -n shortly
kubectl logs -n shortly -l component=backend
kubectl logs -n shortly -l component=frontend
kubectl logs -n shortly -l component=redis
```

### Services prüfen:
```bash
kubectl get svc -n shortly
# Erwartet:
# - shortly-shortly-frontend (ClusterIP:80)
# - shortly-shortly-backend (ClusterIP:8080)
# - shortly-shortly-redis (ClusterIP:6379)
```

### Ingress prüfen:
```bash
kubectl get ingress -n shortly
kubectl describe ingress -n shortly shortly-shortly-ingress
```

### Backend Health:
```bash
kubectl port-forward -n shortly svc/shortly-shortly-backend 8080:8080
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

### Frontend:
```bash
kubectl port-forward -n shortly svc/shortly-shortly-frontend 4200:80
curl http://localhost:4200
```

### Redis:
```bash
kubectl port-forward -n shortly svc/shortly-shortly-redis 6379:6379
redis-cli ping
```

## Monitoring (falls kube-prometheus-stack installiert)

ServiceMonitor wird automatisch erstellt wenn:
- `monitoring.serviceMonitor.enabled: true`
- Prometheus Operator installiert ist
- Label `release: kube-prometheus-stack` matcht

Überprüfen:
```bash
kubectl get servicemonitor -n monitoring
kubectl get servicemonitor -n shortly
```

## Troubleshooting

### ImagePullBackOff bei Frontend/Backend:
```bash
# Secret prüfen
kubectl get secret gitlab-registry -n shortly

# Secret neu erstellen mit richtigem Token
kubectl delete secret gitlab-registry -n shortly
kubectl create secret docker-registry gitlab-registry \
  --docker-server=projectregistry.neusta.de \
  --docker-username=<USERNAME> \
  --docker-password=<NEUES_TOKEN> \
  --docker-email=<EMAIL> \
  -n shortly

# ArgoCD neu syncen
kubectl delete pod -n shortly -l component=backend
kubectl delete pod -n shortly -l component=frontend
```

### Redis ErrImagePull:
- Wurde durch expliziten `docker.io/redis:8-alpine` Pfad behoben
- Falls weiterhin Probleme: Anderes Registry-Mirror verwenden oder lokal cachen

### Backend kann Redis nicht erreichen:
```bash
# Service-Name prüfen
kubectl get svc -n shortly | grep redis

# DNS-Auflösung testen
kubectl exec -n shortly -it <backend-pod> -- nslookup shortly-shortly-redis

# Umgebungsvariablen prüfen
kubectl exec -n shortly -it <backend-pod> -- env | grep REDIS
```

### Ingress zeigt nichts an:
```bash
# NGINX Ingress Controller installiert?
kubectl get pods -n ingress-nginx

# Ingress-Klasse vorhanden?
kubectl get ingressclass

# Ingress Events prüfen
kubectl describe ingress -n shortly
```

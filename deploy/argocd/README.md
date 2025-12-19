# ArgoCD Deployment für Shortly

Diese Anleitung beschreibt, wie du die Shortly-Anwendung mit ArgoCD auf einem Kubernetes-Cluster (z.B. Docker Desktop)
deployst.

## Voraussetzungen

- Kubernetes Cluster (z.B. Docker Desktop mit aktiviertem Kubernetes)
- kubectl CLI installiert
- argocd CLI installiert (optional, für erweiterte Operationen)
- Zugriff auf die GitLab Registry projectregistry.neusta.de

## 1. ArgoCD installieren

Falls ArgoCD noch nicht installiert ist:

```bash
# Namespace erstellen
kubectl create namespace argocd

# ArgoCD installieren
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Warten bis alle Pods laufen
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=300s
```

### ArgoCD UI zugänglich machen

```bash
# Port-Forward für ArgoCD UI
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Zugriff: https://localhost:8080

### Initial Admin Password abrufen

```bash
# Windows PowerShell
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | ForEach-Object { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }

# Linux/Mac/Git Bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

Login: admin / <password aus obigem Befehl>

## 2. GitLab Registry Secret erstellen

Damit Kubernetes Images aus der privaten GitLab Registry pullen kann:

```bash
# Namespace erstellen
kubectl create namespace shortly

# Registry Secret erstellen
kubectl create secret docker-registry gitlab-registry   --docker-server=projectregistry.neusta.de   --docker-username=<DEIN_GITLAB_USERNAME>   --docker-password=<DEIN_GITLAB_TOKEN>   --docker-email=<DEINE_EMAIL>   -n shortly

# Optional: Secret auch in argocd namespace
kubectl create secret docker-registry gitlab-registry   --docker-server=projectregistry.neusta.de   --docker-username=<DEIN_GITLAB_USERNAME>   --docker-password=<DEIN_GITLAB_TOKEN>   --docker-email=<DEINE_EMAIL>   -n argocd
```

## 3. NGINX Ingress Controller installieren

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

# Warten bis Ingress Controller läuft
kubectl wait --namespace ingress-nginx   --for=condition=ready pod   --selector=app.kubernetes.io/component=controller   --timeout=300s
```

## 4. Prometheus + Grafana Stack installieren (optional)

```bash
# Namespace erstellen
kubectl create namespace monitoring

# Helm Repo hinzufügen
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# kube-prometheus-stack installieren
helm upgrade --install kube-prometheus-stack   prometheus-community/kube-prometheus-stack   --namespace monitoring   --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false

#Check its status by running:
kubectl --namespace monitoring get pods -l "release=kube-prometheus-stack"

#Get Grafana 'admin' user password by running:

kubectl --namespace monitoring get secrets kube-prometheus-stack-grafana -o jsonpath="{.data.admin-password}" | base64 -d ; echo

#Access Grafana local instance:

export POD_NAME=$(kubectl --namespace monitoring get pod -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=kube-prometheus-stack" -oname)
kubectl --namespace monitoring port-forward $POD_NAME 3000

# Get your grafana admin user password by running:

kubectl get secret --namespace monitoring -l app.kubernetes.io/component=admin-secret -o jsonpath="{.items[0].data.admin-password}" | base64 --decode ; echo
```

## 5. Shortly Application über ArgoCD deployen

```bash
# ArgoCD Application und Project erstellen
kubectl apply -f shortly-project.yaml
kubectl apply -f shortly-app.yaml
```

## 6. Deployment überprüfen

```bash
# Pods prüfen
kubectl get pods -n shortly

# Services prüfen
kubectl get svc -n shortly

# Ingress prüfen
kubectl get ingress -n shortly

# Logs ansehen
kubectl logs -n shortly -l component=backend
kubectl logs -n shortly -l component=frontend
```

## 7. Applikation aufrufen

### Via Ingress (empfohlen)

Für Docker Desktop: http://localhost

### Via Port-Forward (Alternative)

```bash
# Frontend
kubectl port-forward -n shortly svc/shortly-frontend 4200:80

# Backend
kubectl port-forward -n shortly svc/shortly-backend 8080:8080
```

Zugriff:

- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- Backend Health: http://localhost:8080/actuator/health
- Backend Metrics: http://localhost:8080/actuator/prometheus

## 8. Updates deployen

ArgoCD synchronisiert automatisch bei Änderungen im Git-Repository.

Manuell synchronisieren:

```bash
argocd app sync shortly
```

## Troubleshooting

### Pods starten nicht

```bash
kubectl describe pod -n shortly <POD_NAME>
kubectl logs -n shortly <POD_NAME>
```

### Backend kann nicht auf Redis zugreifen

```bash
kubectl get svc -n shortly shortly-redis
kubectl logs -n shortly -l component=redis
```

## Konfiguration

Die Konfiguration wird über values-docker-desktop.yaml gesteuert:

- **Image Tags**: frontend.image.tag und backend.image.tag
- **Replicas**: frontend.replicaCount und backend.replicaCount
- **Resources**: CPU/Memory Limits
- **Ingress**: Host und Annotations
- **Redis**: Persistence und Resources
- **Monitoring**: ServiceMonitor aktivieren

## Deinstallation

```bash
kubectl delete -f shortly-app.yaml
kubectl delete namespace shortly
```

## Wichtige URLs

- **ArgoCD UI**: https://localhost:8080 (via Port-Forward)
- **Grafana**: http://localhost:3000 (via Port-Forward)
- **Frontend**: http://localhost (via Ingress)
- **Backend API**: http://localhost/api (via Ingress)

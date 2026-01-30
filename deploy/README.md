# Deploy (Helm + Kubernetes + ArgoCD + OpenTofu)

This directory contains everything needed to run Shortly in a Kubernetes cluster.
The README targets beginners and describes both the recommended OpenTofu flow
and a manual installation.

## Structure

- `deploy/helm/shortly`: Helm chart for frontend, backend, Redis, ingress, and monitoring
- `deploy/argocd`: ArgoCD Application/Project + values for Docker Desktop
- `deploy/tofu`: OpenTofu code for cluster bootstrap (namespaces, ArgoCD, ingress, monitoring)

## Quick start with OpenTofu (recommended)

Prerequisites: Kubernetes cluster running (e.g. Docker Desktop), `kubectl`, `helm`, and `tofu` installed.

```bash
cd deploy/tofu
cp terraform.tfvars.example terraform.tfvars
# fill in terraform.tfvars (GitLab user/token/email, optional flags)
tofu init
tofu apply
```

Open ArgoCD UI:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Initial admin password (PowerShell):

```powershell
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | ForEach-Object { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }
```

Grafana (if monitoring was installed):

```bash
kubectl port-forward svc/kube-prometheus-stack-grafana -n monitoring 3000:80
```

Check status:

```bash
kubectl get pods -n shortly
kubectl get svc -n shortly
kubectl get ingress -n shortly
```

## ArgoCD: How the deployment is organized

- ArgoCD pulls the repo and deploys the Helm chart from `deploy/helm/shortly`.
- The application lives in `deploy/argocd/shortly-app.yaml`.
- For Docker Desktop, additional values from `deploy/argocd/values-docker-desktop.yaml` are loaded.
- If you use your own repo, adjust `repoURL` in `deploy/argocd/shortly-app.yaml`.

## Manual setup (without OpenTofu)

### 1) Build and push images

```bash
docker build -t YOUR_REGISTRY/shortly-frontend:0.0.1 shortly-frontend
docker build -t YOUR_REGISTRY/shortly-backend:0.0.1 shortly-java
docker push YOUR_REGISTRY/shortly-frontend:0.0.1
docker push YOUR_REGISTRY/shortly-backend:0.0.1
```

### 2) Install ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=argocd-server -n argocd --timeout=300s
```

### 3) Registry secret (only if private registry)

```bash
kubectl create namespace shortly
kubectl create secret docker-registry gitlab-registry \
  --docker-server=projectregistry.neusta.de \
  --docker-username=<YOUR_GITLAB_USERNAME> \
  --docker-password=<YOUR_GITLAB_TOKEN> \
  --docker-email=<YOUR_EMAIL> \
  -n shortly
```

### 4) Install ingress controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=300s
```

### 5) Monitoring (optional)

```bash
kubectl create namespace monitoring
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false
```

### 6) Create ArgoCD application

```bash
kubectl apply -f deploy/argocd/shortly-project.yaml
kubectl apply -f deploy/argocd/shortly-app.yaml
```

## Direct Helm deployment (without ArgoCD)

```bash
kubectl create namespace shortly
helm upgrade --install shortly deploy/helm/shortly \
  --namespace shortly \
  -f deploy/helm/shortly/values.yaml \
  -f deploy/argocd/values-docker-desktop.yaml \
  --set frontend.image.repository=YOUR_REGISTRY/shortly-frontend \
  --set frontend.image.tag=0.0.1 \
  --set backend.image.repository=YOUR_REGISTRY/shortly-backend \
  --set backend.image.tag=0.0.1
```

## Important values

- `frontend.image.repository`, `frontend.image.tag`
- `backend.image.repository`, `backend.image.tag`
- `ingress.enabled`, `ingress.className`, `ingress.hosts`
- `redis.enabled`, `redis.persistence.enabled`
- `monitoring.serviceMonitor.enabled`, `monitoring.serviceMonitor.additionalLabels.release`

## Useful commands

```bash
kubectl get pods -n shortly
kubectl logs -n shortly -l component=backend
kubectl logs -n shortly -l component=frontend
kubectl port-forward -n shortly svc/shortly-backend 8080:8080
kubectl port-forward -n shortly svc/shortly-frontend 4200:80
```

## Uninstall

```bash
# Remove ArgoCD app
kubectl delete -f deploy/argocd/shortly-app.yaml
kubectl delete namespace shortly

# Full cleanup (if OpenTofu was used)
cd deploy/tofu
tofu destroy
```

# Cluster Setup mit OpenTofu / Terraform

Diese Konfiguration automatisiert das Setup des Kubernetes-Clusters (z.B. Docker Desktop) mit OpenTofu (einem
Open-Source Fork von Terraform).

## Voraussetzungen

- [OpenTofu](https://opentofu.org/docs/intro/install/) (oder Terraform) installiert.
- Kubernetes Cluster (z.B. Docker Desktop) läuft.
- GitLab Personal Access Token mit `read_registry` Berechtigung.

## Verwendung

1. **In das Verzeichnis wechseln**:
   ```bash
   cd deploy/tofu
   ```

2. **Variablen konfigurieren**:
   Kopiere die Beispiel-Variablen und fülle sie aus:
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   ```
   Editierte die `terraform.tfvars` mit deinem GitLab-User und Token.

3. **Initialisieren**:
   ```bash
   tofu init
   ```

4. **Planen (optional)**:
   ```bash
   tofu plan
   ```

5. **Anwenden**:
   ```bash
   tofu apply
   ```
   Bestätige mit `yes`.

## Was wird installiert?

- **Namespaces**: `argocd`, `shortly`, `monitoring`, `ingress-nginx`.
- **GitLab Registry Secrets**: In den benötigten Namespaces.
- **ArgoCD**: Installiert via Helm.
- **NGINX Ingress Controller**: Installiert via Helm.
- **Monitoring Stack**: Prometheus & Grafana via Helm.
- **ArgoCD Project & App**: Registriert die Shortly-Anwendung automatisch.

## Nach dem Setup

Um auf die UIs zuzugreifen, musst du Port-Forwarding verwenden (falls kein echter LoadBalancer vorhanden ist):

### ArgoCD UI

```powershell
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Login: `admin` / Passwort siehe Output von `tofu apply`.

### Grafana

```powershell
kubectl port-forward svc/kube-prometheus-stack-grafana -n monitoring 3000:80
```

## Deinstallation

Um alle erstellten Ressourcen wieder zu entfernen:

```bash
tofu destroy
```

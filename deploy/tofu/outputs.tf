output "argocd_url" {
  value       = "https://localhost:8080 (nach Port-Forward)"
  description = "URL für ArgoCD UI"
}

output "grafana_url" {
  value       = "http://localhost:3000 (nach Port-Forward)"
  description = "URL für Grafana"
}

output "app_url" {
  value       = "http://localhost"
  description = "URL für die Shortly Anwendung (via Ingress)"
}

output "get_argocd_password_cmd" {
  value       = "kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath=\"{.data.password}\" | ForEach-Object { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }"
  description = "Befehl zum Abrufen des initialen ArgoCD Admin-Passworts (PowerShell)"
}

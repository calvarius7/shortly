# Helm repository initialization (Windows workaround)
# This ensures helm repos are available before deploying charts

resource "null_resource" "helm_repo_init" {
  provisioner "local-exec" {
    command = <<-EOT
      helm repo add argo https://argoproj.github.io/argo-helm --force-update
      helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx --force-update
      helm repo add prometheus-community https://prometheus-community.github.io/helm-charts --force-update
      helm repo add grafana https://grafana.github.io/helm-charts --force-update
      helm repo update
    EOT
  }

  triggers = {
    always_run = timestamp()
  }
}

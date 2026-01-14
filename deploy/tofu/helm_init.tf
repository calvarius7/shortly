# Helm repository initialization (Windows workaround)
# This ensures helm repos are available before deploying charts

resource "null_resource" "helm_repo_init" {
  provisioner "local-exec" {
    command = "helm repo add argo https://argoproj.github.io/argo-helm --force-update && helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx --force-update && helm repo add prometheus-community https://prometheus-community.github.io/helm-charts --force-update && helm repo update"
  }

  triggers = {
    # Run once per tofu init
    repos = "argo,ingress-nginx,prometheus-community"
  }
}

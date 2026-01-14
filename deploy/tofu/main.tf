# Namespaces
resource "kubernetes_namespace" "argocd" {
  metadata {
    name = "argocd"
  }
}

resource "kubernetes_namespace" "shortly" {
  metadata {
    name = "shortly"
  }
}

resource "kubernetes_namespace" "monitoring" {
  count = var.install_monitoring ? 1 : 0
  metadata {
    name = "monitoring"
  }
}

resource "kubernetes_namespace" "ingress_nginx" {
  count = var.install_ingress ? 1 : 0
  metadata {
    name = "ingress-nginx"
  }
}

# GitLab Registry Secrets
locals {
  gitlab_auth = base64encode("${var.gitlab_user}:${var.gitlab_token}")
  docker_config = jsonencode({
    auths = {
      "projectregistry.neusta.de" = {
        auth     = local.gitlab_auth
        user     = var.gitlab_user
        password = var.gitlab_token
        email    = var.gitlab_email
      }
    }
  })
}

resource "kubernetes_secret" "gitlab_registry" {
  for_each = toset(["gitlab-registry", "gitlab-registry2", "neusta-registry"])
  
  metadata {
    name      = each.key
    namespace = kubernetes_namespace.shortly.metadata[0].name
  }

  type = "kubernetes.io/dockerconfigjson"

  data = {
    ".dockerconfigjson" = local.docker_config
  }
}

# Helm Release: ArgoCD
resource "helm_release" "argocd" {
  depends_on = [null_resource.helm_repo_init]

  name       = "argocd"
  repository = "https://argoproj.github.io/argo-helm"
  chart      = "argo-cd"
  namespace  = kubernetes_namespace.argocd.metadata[0].name
  version    = "5.51.6" # Stable version

  set {
    name  = "server.service.type"
    value = "ClusterIP"
  }
}

# Helm Release: Ingress Nginx
resource "helm_release" "ingress_nginx" {
  depends_on = [null_resource.helm_repo_init]
  count      = var.install_ingress ? 1 : 0
  name       = "ingress-nginx"
  repository = "https://kubernetes.github.io/ingress-nginx"
  chart      = "ingress-nginx"
  namespace  = kubernetes_namespace.ingress_nginx[0].metadata[0].name
  version    = "4.8.3"

  set {
    name  = "controller.service.type"
    value = "LoadBalancer"
  }
}

# Helm Release: Prometheus/Grafana Stack
resource "helm_release" "monitoring" {
  depends_on = [null_resource.helm_repo_init]
  count      = var.install_monitoring ? 1 : 0
  name       = "kube-prometheus-stack"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  namespace  = kubernetes_namespace.monitoring[0].metadata[0].name
  version    = "55.0.0"

  set {
    name  = "prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues"
    value = "false"
  }
}

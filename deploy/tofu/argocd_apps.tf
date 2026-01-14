# GitLab Repository Secret for ArgoCD
resource "kubernetes_secret" "argocd_gitlab_repo" {
  metadata {
    name      = "gitlab-repo-creds"
    namespace = kubernetes_namespace.argocd.metadata[0].name
    labels = {
      "argocd.argoproj.io/secret-type" = "repository"
    }
  }

  data = {
    type     = "git"
    url      = "https://gitlab.neusta.de/limmoor/shortly.git"
    username = var.gitlab_user
    password = var.gitlab_token
  }

  depends_on = [helm_release.argocd]
}

resource "kubectl_manifest" "shortly_project" {
  yaml_body = file("${path.module}/../argocd/shortly-project.yaml")

  depends_on = [helm_release.argocd, kubernetes_secret.argocd_gitlab_repo]
}

resource "kubectl_manifest" "shortly_app" {
  yaml_body = file("${path.module}/../argocd/shortly-app.yaml")

  depends_on = [helm_release.argocd, kubectl_manifest.shortly_project]
}

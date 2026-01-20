variable "kube_config_path" {
  type        = string
  description = "Path to the kubeconfig file"
  default     = "~/.kube/config"
}

variable "kube_config_context" {
  type        = string
  description = "The kubernetes context to use"
  default     = "docker-desktop"
}

variable "gitlab_user" {
  type        = string
  description = "GitLab Username"
}

variable "gitlab_token" {
  type        = string
  description = "GitLab Personal Access Token"
  sensitive   = true
}

variable "gitlab_email" {
  type        = string
  description = "GitLab Email"
}

variable "install_monitoring" {
  type    = bool
  default = true
}

variable "install_ingress" {
  type    = bool
  default = true
}

variable "install_loki" {
  type        = bool
  default     = true
  description = "Install Loki for log aggregation"
}

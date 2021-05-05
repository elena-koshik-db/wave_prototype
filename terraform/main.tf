provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

resource "google_pubsub_topic" "topic" {
  name = "job-topic"
}

resource "google_cloud_scheduler_job" "job" {
  name        = "test-job"
  description = "test job"
  schedule    = "* * * * *"

  pubsub_target {
    topic_name = google_pubsub_topic.topic.id
    data       = base64encode("test")
  }
}

resource "google_dataflow_job" "pub_sub_dataflow_job" {
  name = "test-terraform-dataflow-job"
  project = var.project_id
  zone = var.zone

  template_gcs_path = var.template_gcs_path
  temp_gcs_location = var.temp_gcs_location

  parameters = {
    inputTopic = google_pubsub_topic.topic.id
  }

  on_delete = "cancel"
}


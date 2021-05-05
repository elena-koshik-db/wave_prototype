variable "project_id" {
  type = string
  default = "sandbox-307310"
}

variable "region" {
  type = string
  default = "europe-west3"
}

variable "zone" {
  type = string
  default = "europe-west3-a"
}

variable "template_gcs_path" {
  type = string
  description = "The GCS path to the Dataflow job template. Expect 'gs://<bucket>/<path>'. Ex: 'gs://my-bucket/templates/template_file'"
  default = "gs://pub_sub_example/templates/pub_sub_template"
}

variable "temp_gcs_location" {
  type = string
  description = "A writeable location on GCS for the Dataflow job to dump its temporary data. Expect 'gs://<bucket>/<path>'. Ex: 'gs://my-bucket/tmp_dir'"
  default     = "gs://pub_sub_example/tmp_dir"
}
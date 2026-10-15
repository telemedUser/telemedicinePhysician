datacenter = "telemedicine"
data_dir   = "/consul/data"
log_level  = "INFO"

ui_config {
  enabled = true
}

server = true
bootstrap_expect = 1

client_addr = "0.0.0.0"
bind_addr   = "0.0.0.0"

telemetry {
  prometheus_retention_time = "60s"
}

connect {
  enabled = false
}

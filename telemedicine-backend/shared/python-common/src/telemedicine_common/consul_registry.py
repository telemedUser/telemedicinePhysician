import atexit
import os
from typing import Optional

import consul


def register_service(
    service_name: str,
    service_port: int,
    health_check_url: str,
    consul_host: Optional[str] = None,
    consul_port: Optional[int] = None,
) -> consul.Consul:
    host = consul_host or os.getenv("CONSUL_HOST", "localhost")
    port = int(consul_port or os.getenv("CONSUL_PORT", "8500"))
    service_id = f"{service_name}-{service_port}"

    client = consul.Consul(host=host, port=port)
    client.agent.service.register(
        name=service_name,
        service_id=service_id,
        port=service_port,
        check=consul.Check.http(url=health_check_url, interval="10s", timeout="5s"),
    )

    def deregister() -> None:
        client.agent.service.deregister(service_id)

    atexit.register(deregister)
    return client

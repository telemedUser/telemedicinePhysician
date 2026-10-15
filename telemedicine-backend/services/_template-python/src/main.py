from contextlib import asynccontextmanager
import os

from fastapi import FastAPI

from telemedicine_common.consul_registry import register_service
from telemedicine_common.dto import ApiResponse

from .routes import router

SERVICE_NAME = "template-python"
SERVICE_PORT = int(os.getenv("SERVICE_PORT", "8082"))
SERVICE_HOST = os.getenv("SERVICE_HOST", "127.0.0.1")


@asynccontextmanager
async def lifespan(app: FastAPI):
    register_service(
        service_name=SERVICE_NAME,
        service_port=SERVICE_PORT,
        health_check_url=f"http://{SERVICE_HOST}:{SERVICE_PORT}/health",
    )
    yield


app = FastAPI(title="Template Python Service", lifespan=lifespan)
app.include_router(router)


@app.get("/health")
def health():
    return {"status": "UP", "service": SERVICE_NAME}

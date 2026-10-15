from fastapi import APIRouter

from telemedicine_common.dto import ApiResponse

router = APIRouter()


@router.get("/hello")
def hello() -> ApiResponse[dict]:
    return ApiResponse.ok(
        {"service": "template-python", "message": "Hello from Python microservice"}
    )

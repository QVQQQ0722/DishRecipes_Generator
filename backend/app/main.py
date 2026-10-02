from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.errors import ImportFailure
from app.pipeline import create_pipeline
from app.schemas import ExampleRequest, ImportResult

app = FastAPI(title="拾味 Python 导入服务", version="0.1.0")


@app.exception_handler(ImportFailure)
async def import_failure_handler(request: Request, error: ImportFailure):
    return JSONResponse(status_code=error.status, content={
        "error": {"code": error.code, "message": error.message},
    })


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/v1/imports/example", response_model=ImportResult)
async def import_example(body: ExampleRequest):
    # Clients cannot choose arbitrary server file paths or model endpoints.
    return await create_pipeline().run(f"fixture://{body.example_id}")

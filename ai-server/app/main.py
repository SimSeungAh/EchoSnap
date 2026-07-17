from fastapi import FastAPI

app = FastAPI(
    title="SmartRecycle AI Server",
    description="SmartRecycle 이미지 분석 서버",
    version="0.1.0",
)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {
        "status": "ok",
        "service": "smart-recycle-ai-server",
    }
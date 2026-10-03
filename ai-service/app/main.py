from fastapi import FastAPI

app = FastAPI(
    title="遗韵 AI Service",
    description="遗韵非遗文化智能创作平台 AI 服务",
    version="0.1.0"
)


@app.get("/health")
def health():
    return {
        "code": 200,
        "message": "ai-service is running"
    }
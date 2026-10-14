from fastapi import FastAPI
from app.api.chat import router as chat_router
from app.rag.retriever import ensure_index_loaded

app = FastAPI(
    title="遗韵 AI Service",
    description="遗韵非遗文化智能创作平台 AI 服务",
    version="0.1.0"
)


@app.on_event("startup")
def on_startup():
    try:
        ensure_index_loaded()
    except Exception as e:
        print(f"[启动警告] FAISS 索引加载失败：{e}")


@app.get("/health")
def health():
    return {
        "code": 200,
        "message": "ai-service is running"
    }


app.include_router(chat_router)
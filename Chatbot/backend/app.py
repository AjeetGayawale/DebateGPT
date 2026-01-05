from fastapi import FastAPI
from pydantic import BaseModel
import requests
import json
from concurrent.futures import ThreadPoolExecutor

app = FastAPI()

executor = ThreadPoolExecutor(max_workers=2)

OLLAMA_URL = "http://localhost:11434/api/generate"

class ChatRequest(BaseModel):
    message: str


def call_ollama_blocking(prompt: str) -> str:
    payload = {
        "model": "phi3",
        "prompt": f"""
You are a debate assistant.
Give a concise counter-argument in 4–5 sentences.

User argument:
{prompt}
""",
        "stream": True
    }

    response = requests.post(
        OLLAMA_URL,
        headers={"Content-Type": "application/json"},
        data=json.dumps(payload),
        stream=True,
        timeout=300
    )

    full_response = ""

    for line in response.iter_lines(decode_unicode=True):
        if line:
            data = json.loads(line)
            if "response" in data:
                full_response += data["response"]
            if data.get("done"):
                break

    return full_response.strip()


@app.post("/chat")
def chat(req: ChatRequest):
    try:
        future = executor.submit(call_ollama_blocking, req.message)
        reply = future.result()

        if not reply:
            return {"reply": "⚠️ Model returned no response."}

        return {"reply": reply}

    except Exception as e:
        print("Backend error:", e)
        return {"reply": "Sorry, I couldn't process that request."}

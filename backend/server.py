"""
FastAPI server for caption generation.

Exposes:
- POST /v1/generate-caption  (multipart/form-data with images[])
- GET  /health               (health check)

Env vars:
- DASHSCOPE_API_KEY: Your DashScope API key (required)

Notes:
- This service adapts the logic from 1.py to handle uploaded images directly.
"""
from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from fastapi.responses import JSONResponse
from typing import List, Optional
import os
import base64
import json
import requests
import asyncio
import io
from PIL import Image

app = FastAPI(title="Caption Service", version="1.0.0")


def clean_text(text: str) -> str:
    """Clean text similar to backend/1.py, keeping only natural prose."""
    import re

    # Remove markdown-like symbols
    text = re.sub(r'[#*\-_`]', '', text)
    text = re.sub(r'---+', '', text)
    text = re.sub(r'===+', '', text)
    text = re.sub(r'\*\*([^*]+)\*\*', r'\1', text)
    text = re.sub(r'\*([^*]+)\*', r'\1', text)
    text = re.sub(r'###\s*\d+\.\s*', '', text)
    text = re.sub(r'###\s*', '', text)
    text = re.sub(r'##\s*', '', text)
    text = re.sub(r'#\s*', '', text)

    # Remove bullets and numbering
    text = re.sub(r'^\s*[-•]\s*', '', text, flags=re.MULTILINE)
    text = re.sub(r'^\s*\d+\.\s*', '', text, flags=re.MULTILINE)


    # Trim whitespace and excessive newlines
    text = re.sub(r'\n\s*\n', '\n\n', text)
    text = re.sub(r'\n{3,}', '\n\n', text)
    text = re.sub(r'^\s+', '', text, flags=re.MULTILINE)
    text = text.strip()
    return text


def _compress_image_bytes(data: bytes, max_side: int = 1280, quality: int = 80) -> bytes:
    """Downscale and recompress image to JPEG to reduce payload size.
    Falls back to original bytes on any error."""
    try:
        with Image.open(io.BytesIO(data)) as img:
            # Convert to RGB to ensure JPEG compatibility
            if img.mode not in ("RGB", "L"):  # keep grayscale as is
                img = img.convert("RGB")

            # Resize keeping aspect ratio if larger than max_side
            w, h = img.size
            if max(w, h) > max_side:
                img.thumbnail((max_side, max_side), Image.LANCZOS)

            # Re-encode to JPEG
            buf = io.BytesIO()
            img.save(buf, format="JPEG", quality=quality, optimize=True)
            return buf.getvalue()
    except Exception:
        return data


async def files_to_b64_list(files: List[UploadFile]) -> List[str]:
    b64_list: List[str] = []
    for f in files:
        if not f.content_type or not f.content_type.startswith("image/"):
            raise HTTPException(status_code=400, detail=f"Unsupported file type: {f.content_type}")
        data = await f.read()
        if not data:
            raise HTTPException(status_code=400, detail="Empty file uploaded")

        # Compress to speed up upload to DashScope and reduce timeout risk
        data = _compress_image_bytes(data, max_side=1280, quality=80)
        b64_list.append(base64.b64encode(data).decode("utf-8"))
    return b64_list


def ask_qwen_about_multiple_images_b64(b64_list: List[str], question: str, system_prompt: Optional[str] = None, *, timeout: int = 120) -> str:
    api_key = "sk-6d57d8b276f748b3bc8a413a81ca680b"
    api_url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}",
    }

    # Build content list: images + question text (as user content)
    content_list: List[dict] = []
    for b64 in b64_list:
        content_list.append({"image": f"data:image/jpeg;base64,{b64}"})
    content_list.append({"text": question})
    # If the caller provided a prompt, place it explicitly into a system-level message
    # so the model treats it as high priority instructions. This avoids fragile
    # heuristics that try to parse prompt out of a single question string.
    system_messages: List[dict] = []
    if system_prompt and system_prompt.strip():
        system_text = system_prompt.strip()
        # If prompt requests English (either Chinese '英文' or English words), append a clear English-only instruction
        if ('english' in system_text.lower() or 'use english' in system_text.lower() or '英文' in system_text):
            print("usr requires English")
            if 'please respond in english' not in system_text.lower():
                system_text = system_text + ' Please respond in English.'
        system_messages.append({"role": "system", "content": [{"text": system_text}]})

    # Build the messages list: system messages first, then the user content containing images + instruction
    messages = []
    messages.extend(system_messages)
    messages.append({"role": "user", "content": content_list})

    payload = {
        "model": "qwen-vl-plus",
        "input": {"messages": messages},
        "parameters": {"max_tokens": 200, "temperature": 0.2},
    }

    try:
        resp = requests.post(api_url, headers=headers, data=json.dumps(payload), timeout=timeout)
    except requests.Timeout:
        raise HTTPException(status_code=504, detail="Upstream timeout")
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Upstream request failed: {e}")

    if resp.status_code != 200:
        # Pass through upstream error body if any
        detail = resp.text
        raise HTTPException(status_code=502, detail=f"Upstream error {resp.status_code}: {detail}")

    result = resp.json()
    # Parse choices
    if 'output' in result and 'choices' in result['output']:
        choices = result['output']['choices']
        if choices:
            message = choices[0].get('message', {})
            content = message.get('content', '')
            if isinstance(content, list) and content:
                if isinstance(content[0], dict) and 'text' in content[0]:
                    return str(content[0]['text'])
            return str(content)
    # If not parseable, return pretty json for debugging
    raise HTTPException(status_code=502, detail="Unexpected upstream response format")


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/v1/generate-caption")
async def generate_caption(images: List[UploadFile] = File(...), prompt: str | None = Form(None)):
    # Basic constraints
    if not images:
        raise HTTPException(status_code=400, detail="No images uploaded")
    if len(images) > 9:
        raise HTTPException(status_code=400, detail="Too many images (max 9)")

    b64_list = await files_to_b64_list(images)
    # Base instruction
    base_instruction = (
        "以上的要求具有最高的优先级，如果一下要求和上述要求相矛盾，请忽略下面的要求"
        "语言自然流畅，如果以上的要求是开心的可以使用表情包，对于伤感的要求，不要使用表情包，请语言简洁，并有诗意；"
        "根据这些图片的内容，生成一段不长于50字的朋友圈文案："
        "删除重复并修正错字/断词；"
        "只输出正文，不要标题、序号、标签或解释。"
    )

    # Build a clear system-level instruction that contains the base rules plus any client prompt.
    # Put the base instruction into the system role (higher priority). The user message will be
    # a short, language-neutral request so we don't accidentally override system-level language
    # constraints by embedding the base instruction inside the user content.
    if prompt and prompt.strip():
        system_combined = f"{prompt.strip()}。{base_instruction}"
    else:
        system_combined = base_instruction

    # If the client explicitly requests English, ensure an unmistakable system-level directive.
    if prompt and ("english" in prompt.lower() or "use english" in prompt.lower() or "英文" in prompt):
        if 'please respond in english' not in system_combined.lower():
            system_combined = system_combined + ' Please respond in English.'

    # Short, neutral user instruction — images are provided in the message content by the caller.
    user_question = "Please follow the system instructions and generate the requested short caption based on the images."

    raw = ask_qwen_about_multiple_images_b64(b64_list, user_question, system_prompt=system_combined)
    if not raw:
        raise HTTPException(status_code=502, detail="Empty response from upstream")
    caption = clean_text(raw)
    # Ensure one-line under 200 chars as a final guard (non-strict)
    caption = caption.replace('\n', ' ').strip()
    if len(caption) > 200:
        caption = caption[:200]
    return JSONResponse(status_code=200, content={"caption": caption})


# Optional: enable running via `python server.py`
if __name__ == "__main__":
    import uvicorn
    # Bind to all interfaces so Android device in LAN can reach it
    uvicorn.run("server:app", host="0.0.0.0", port=8000, reload=False)

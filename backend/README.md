# Caption Backend Service

FastAPI HTTP 服务，将多张图片上传至千问 Qwen-VL（DashScope）生成一段精炼文案，返回 JSON：{"caption": "..."}。

## 接口

- POST `/v1/generate-caption`
  - Content-Type: `multipart/form-data`
  - 字段：`images`（可多次，图片文件，最多 9 张）
  - 成功：`200 { "caption": "..." }`
  - 失败：`4xx/5xx { "detail": "..." }`
- GET `/health`：健康检查

## 环境准备（Windows PowerShell）

```powershell
# 进入 backend 目录
cd backend

# 创建并激活虚拟环境
python -m venv .venv
. .\.venv\Scripts\Activate.ps1

# 安装依赖
pip install -r requirements.txt

# 设置 DashScope API Key（仅当前会话生效）
$env:DASHSCOPE_API_KEY = "你的_DashScope_Key"

# 启动服务（监听 0.0.0.0 便于同局域网 Android 访问）
python server.py
# 或：
# uvicorn server:app --host 0.0.0.0 --port 8000 --workers 1
```

启动后，服务默认在 `http://<你的电脑IP>:8000/`。Android 前端将 `baseUrl` 指向该地址。

## 与 Android 前端对接

- Retrofit 接口示例：
  - POST `/v1/generate-caption`
  - `@Multipart @POST("v1/generate-caption") suspend fun generateCaption(@Part images: List<MultipartBody.Part>): CaptionResponse`
  - 字段名务必使用 `images`（与本服务一致）。
- AndroidManifest 需包含：`<uses-permission android:name="android.permission.INTERNET"/>`
- 建议 OkHttp 超时：90~120s；上传前对大图进行压缩（最长边 1280~1920，JPEG 质量 80）。

## 部署与放置建议

你要“做一个软件”时，后端程序可以这样放：

1) 本地/内网部署（开发或家庭/办公室使用）
- 将 `backend` 文件夹部署到一台 Windows 电脑或服务器上。
- 创建 `.venv` 并安装依赖，配置 `DASHSCOPE_API_KEY` 为系统环境变量。
- 配置 Windows 任务计划或 NSSM/WinSW，把 `uvicorn`/`python server.py` 做成自启动服务。
- 确保防火墙放行端口（默认 8000）。Android 使用 `http://<该机器局域网IP>:8000/` 作为 `baseUrl`。

2) 服务器部署（对公网/多用户）
- 选择云主机或容器：Docker + Uvicorn/Gunicorn + 反向代理（Nginx/Caddy），开启 HTTPS。
- 将 `DASHSCOPE_API_KEY` 配置为容器/主机环境变量，避免写入仓库。
- 增加限流/鉴权（如 API key、JWT），监控日志与告警，设置合理的并发与超时。

3) 桌面应用内置后端（Windows）
- 将 `backend` 与你的应用一起打包发布：
  - 方案A：保留 Python 运行时（如便携式 Python + .venv），应用启动时拉起 `uvicorn` 监听 127.0.0.1:8000；
  - 方案B：用 PyInstaller 把 `server.py` 打包为独立 exe；应用随进程启动/退出；
- 在应用内请求 `http://127.0.0.1:8000/`；第一次启动提示设置 `DASHSCOPE_API_KEY`（写入系统环境变量或本地安全存储）。

> 注意：DashScope API Key 必须只放在后端，不要进入 APK 或前端资源。

## 目录备注

- `server.py`：HTTP 服务入口
- `requirements.txt`：依赖清单
- `1.py`：原有离线脚本（保留），可作为单机批处理使用，不对外提供 HTTP
- `out.txt`：示例输出（来自离线脚本）

## 简单自检

- 健康检查：`GET http://127.0.0.1:8000/health`
- 接口测试（以 curl 为例）：
  - `curl -F "images=@a.jpg" -F "images=@b.jpg" http://127.0.0.1:8000/v1/generate-caption`

## 常见问题

- 502/504：上游模型超时或返回异常，降低图片数量与分辨率，检查网络与配额。
- 400 Unsupported file type：确保上传的是图片且 Content-Type 正确。
- 500 Missing env DASHSCOPE_API_KEY：未正确配置环境变量。

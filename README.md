# XApi

一个用于聚合获取财经博主帖子的网站。

## 功能

- Spring Boot 后端封装 X UserTweets GraphQL 请求。
- Redis 缓存、stale 缓存、分布式锁和 X API 限流保护。
- React + Vite 前端浏览推文、快捷切换财经博主。
- 单条推文中文翻译，优先使用 LibreTranslate-compatible 服务，失败时兜底 MyMemory。

## 本地运行

启动 Redis：

```powershell
docker compose up -d redis
```

配置环境变量：

```powershell
$env:X_BEARER_TOKEN="your bearer token"
$env:X_COOKIE="your x cookie"
$env:X_CSRF_TOKEN="your ct0 csrf token"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD="your redis password"
$env:TRANSLATION_BASE_URL="https://libretranslate.com/translate"
$env:TRANSLATION_API_KEY=""
```

启动后端：

```powershell
mvn spring-boot:run
```

启动前端：

```powershell
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173/`，后端默认运行在 `http://localhost:8080/`。

## API

获取用户帖子：

```http
GET /api/x/user-tweets?userId=902839045356744704&count=20
GET /api/x/user-tweets?userId=902839045356744704&count=20&cursor=上一页nextCursor
GET /api/x/user-tweets?userId=902839045356744704&count=20&raw=true
```

翻译文本：

```http
POST /api/x/translate
Content-Type: application/json

{
  "text": "Markets are moving",
  "sourceLang": "en",
  "targetLang": "zh-CN"
}
```

## 测试

```powershell
mvn test
cd frontend
npm run build
```

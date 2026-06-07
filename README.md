# Spring Boot X UserTweets 封装

把这些 Java 文件放到你的 Spring Boot 项目包路径下，例如：

```text
src/main/java/com/example/xapi/
```

## 项目结构

```text
spring-boot-xapi/
  pom.xml
  README.md
  docker-compose.yml
  application.yml.example
  src/
    main/java/com/example/xapi/
      api/                           # Controller、异常处理、API 异常
      cache/                         # Redis 缓存、分布式锁、限流状态
      config/                        # Spring 配置和 x.api 配置项
      dto/                           # 接口响应 DTO
      service/                       # UserTweets 编排逻辑
      upstream/                      # X API 上游请求封装
      XApiApplication.java           # 启动类
    main/resources/application.yml   # 本地配置
    test/java/com/example/xapi/      # 缓存和限流测试
  archive/initial-snippets/          # 早期生成的单文件副本，仅作参考
  logs/                              # 本地运行日志
```

需要的基础依赖：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

配置示例见 `application.yml.example`。敏感值建议通过环境变量注入：

```powershell
$env:X_BEARER_TOKEN="你的 bearer token"
$env:X_COOKIE="你的 cookie"
$env:X_CSRF_TOKEN="你的 ct0 csrf token"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD="你的 Redis requirepass，没有密码则留空"
```

本示例项目已经补齐 Maven 结构，可以直接运行：

```powershell
cd C:\Users\lu\OneDrive\Desktop\投资理财\spring-boot-xapi
docker compose up -d redis
mvn -DskipTests package
java -jar target\spring-boot-xapi-0.0.1-SNAPSHOT.jar
```

调用接口：

```http
GET /api/x/user-tweets?userId=1940360837547565056&count=20
GET /api/x/user-tweets?userId=1940360837547565056&count=20&cursor=上一页nextCursor
GET /api/x/user-tweets?userId=1940360837547565056&count=20&raw=true
```

默认不返回原始 X JSON，响应只包含整理后的字段。需要排查接口字段时，加 `raw=true`。

## 缓存和限流保护

接口使用 Redis 做多实例共享缓存、分布式锁和 X API rate limit 状态保存：

- fresh 缓存默认 5 分钟：`x.api.cache-ttl`
- stale 缓存默认 30 分钟：`x.api.stale-ttl`
- 分布式锁默认 15 秒：`x.api.lock-ttl`
- 等待其他实例刷新默认 2 秒：`x.api.lock-wait`
- 上游剩余额度小于等于 3 时停止刷新：`x.api.rate-limit-min-remaining`
- 限流重置时间后额外保护 30 秒：`x.api.rate-limit-safety-window`

返回会包含缓存元信息：

```json
{
  "cache": {
    "hit": true,
    "stale": false,
    "ttlSeconds": 287,
    "key": "x:user-tweets:..."
  }
}
```

当 X API 限流接近红线时，接口优先返回 stale 缓存；没有 stale 缓存时返回 `429 Too Many Requests`。Redis 不可用时返回 `503`，不会绕过缓存直接打 X。

本地验证命令：

```powershell
curl.exe "http://localhost:8080/api/x/user-tweets?userId=1940360837547565056&count=20"
```

如果你不想暴露 Controller，可以只保留 `XUserTweetsService`，在自己的业务 Service 里注入调用。

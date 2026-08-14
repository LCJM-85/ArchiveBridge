# Redis 接入指南（方案文档）

> **状态：本文件仅为实施方案，当前仓库代码未做任何改动。**
> 按本文档执行后即可完成：验证码/登录限流迁移到 Redis（第 1 层）+ Dashboard/维度表查询缓存（第 2 层）。

---

## 1. 背景与目标

当前项目后端（Spring Boot 3.5）存在两个问题：

| 现状 | 问题 |
|---|---|
| 验证码存在 `HttpSession`（`LoginController`） | 与"无状态 JWT"设计矛盾；多实例部署时生成与校验打到不同实例会失效；重启即丢 |
| 登录/验证码限流用 `ConcurrentHashMap` | 纯 JVM 内存，只对单机有效；重启清零 |
| Dashboard 每次全量聚合 SQL + 遍历文件系统 | 报表数据低频变化、高频读取，白查库 |
| 维度表整表被多个下拉/列表页反复全量查询 | 同上 |

引入 Redis 后：

- **第 1 层（临时状态）**：验证码、限流计数存 Redis → 天然支持多实例、重启不丢、内存不膨胀。
- **第 2 层（查询缓存）**：Dashboard 统计 + 维度表列表缓存 → 减少数据库压力。

> 本项目将 Redis 全部当缓存/临时状态用，**不需要 RDB/AOF 持久化**（`appendonly no`）。数据丢失可回源重建，这是设计前提。

---

## 2. 总体架构

```
前端 (Login.vue / Dashboard.vue ...)
  → Java 控制器 / 服务
       ├─ StringRedisTemplate ──→ Redis (localhost:6379 本地 / redis:6379 docker)
       └─ MyBatis-Plus ──→ PostgreSQL
```

Key 命名规范（统一前缀，全部带 TTL，防止无界增长）：

| Key | 用途 | TTL |
|---|---|---|
| `scau:auth:captcha:{uuid}` | 验证码答案 | 120s |
| `scau:auth:login:fail:{ip}:{user}` | 登录失败计数 | 600s |
| `scau:auth:captcha:req:{ip}` | 验证码请求限流 | 60s |
| `scau:cache:dashboard` | Dashboard 统计 | 300s |
| `scau:cache:dim:province` / `college` / `major` / `class` / `destination` | 维度表整表（degree 表太小且查询口径不同，不缓存，见 4.4） | 1800s |

---

## 3. 本地开发环境提供 Redis（三选一）

### 方案 A：Docker（推荐，和线上环境一致）

```bash
docker run -d --name scau-redis -p 6379:6379 redis:7
# 验证
docker exec scau-redis redis-cli ping   # 返回 PONG
```

### 方案 B：Memurai（Windows 原生，Redis 兼容）

从 https://www.memurai.com/ 下载安装 Developer 版，默认端口 6379，开箱即用。

### 方案 C：WSL

```bash
sudo apt update && sudo apt install -y redis-server
sudo service redis-server start
redis-cli ping   # 返回 PONG
```

> 若本机已存在 Redis（如端口非 6379 或带密码），只需在环境变量 `REDIS_HOST/REDIS_PORT/REDIS_PASSWORD` 中按实际填写，代码无需改动（见第 6 节）。

---

## 4. 后端代码改动

### 4.1 依赖（`scau-archive-insight/pom.xml`）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

### 4.2 配置（`application.yaml`）

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 2s
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

> 沿用项目现有 `${VAR:default}` 风格，未设置环境变量时本地默认 `localhost:6379` 无密码，开箱即用。

### 4.3 第 1 层：改造 `LoginController`

**验证码生成**（`GET /api/captcha`）——从"存 session + 输出图片流"改为"存 Redis + 返回 JSON"：

```java
String uuid = UUID.randomUUID().toString().replace("-", "");
LineCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 38, 4, 20);
stringRedisTemplate.opsForValue().set(
        "scau:auth:captcha:" + uuid, captcha.getCode(), Duration.ofSeconds(120));
// 响应：{ code:200, data:{ uuid: uuid, imageBase64: captcha.getImageBase64Data() } }
```

**验证码校验**（登录时）——前端带 `uuid`，用 `GETDEL`（取走即删，防并发复用；Spring Data Redis 3.x 对应 `opsForValue().getAndDelete(key)`，若用老版本则 `GET` + `DEL` 两步）：

```java
String code = stringRedisTemplate.opsForValue().getAndDelete("scau:auth:captcha:" + uuid);
if (code == null) { /* 验证码不存在或已过期 */ }
if (!code.equalsIgnoreCase(inputCaptcha)) { /* 验证码错误 */ }
```

**登录失败限流**——`INCR + EXPIRE`（首次失败时设窗口），替代 `ConcurrentHashMap`：

```java
String key = "scau:auth:login:fail:" + clientIp + ":" + username.trim().toLowerCase();
Long count = stringRedisTemplate.opsForValue().increment(key);
if (count != null && count == 1L) {
    stringRedisTemplate.expire(key, Duration.ofMinutes(10));   // 10 分钟窗口
}
if (count != null && count >= 8L) { /* 返回 429 拒绝 */ }
// 登录成功时：stringRedisTemplate.delete(key);  // 清零
```

**验证码请求限流**同理：`scau:auth:captcha:req:{ip}`，60s 窗口、上限 30 次。

**删除**：`AttemptWindow` 内部类、两个 `ConcurrentHashMap`、所有 `request.getSession()` 调用。

**Redis 不可用时的降级策略**（务必实现，用 try-catch 包裹 Redis 操作）：

| 功能 | 策略 | 原因 |
|---|---|---|
| 验证码生成/校验 | **fail-closed**（抛 500"验证码服务异常"） | 宁可不可登录，不能无验证码 |
| 登录/验证码限流 | **fail-open**（放行 + `log.warn`） | 限流是保护不是功能，Redis 挂了不能把用户全锁死 |

### 4.4 第 2 层：新建 `CacheService`

新建 `edu.scau.scauarchiveinsight.service.CacheService`，基于 `StringRedisTemplate` + Jackson（存 JSON 字符串，避免 JDK 序列化问题）：

```java
// 核心方法（全部 try-catch，Redis 异常时静默降级为直接查库）
public <T> T get(String key, Class<T> type)            // 读缓存，异常返回 null
public void put(String key, Object value, Duration ttl) // 写缓存，异常忽略
public void evict(String... keys)                       // DEL，key 不存在无害（幂等）
public void evictDashboard()                            // 删 scau:cache:dashboard
public void evictDim(String table)                      // 删 scau:cache:dim:{table}
public void evictAllDims()
```

**Dashboard 缓存**（`DashboardService.getStats()`）——cache-aside：

```java
Map<String, Object> cached = cacheService.get(CacheService.KEY_DASHBOARD, Map.class);
if (cached != null) return cached;
// ... 原有聚合查询逻辑不变 ...
cacheService.put(CacheService.KEY_DASHBOARD, result, Duration.ofMinutes(5));
return result;
```

**维度表列表缓存**——对以下"读整表"的接口做 cache-aside（只缓存列表接口，**不要**缓存 `fuzzyLookupXxx` 等内部查询，它们需要最新数据且量小）：

| 文件 | 方法 | 说明 |
|---|---|---|
| `StudentService` | `listProvinces` / `listMajors` / `listClasses` | 数据管理页下拉 |
| `AdmissionService` | `listProvinces` / `listMajors` | 数据管理页下拉 |
| `GraduationService` | `listDestinations` | 数据管理页下拉 |
| `CollegeService` | `list(keyword)` | **仅 keyword 为空时缓存**，带关键字搜索直接查库 |
| `MajorService` | `list(keyword)` | 同上；JSON 内已含 college/degree 名称映射，evict 时一并失效 |
| `ClassService` | `list(keyword)` | 同上；JSON 内已含 major 名称映射 |

> ⚠ **degree 不缓存**：`StudentService.listDegrees`（只含 学士/硕士/博士 三个层次）与 `GraduationService.listDegrees`（全部具体学位）**数据集不同**，若共用同一 key 会互相污染导致下拉数据串页；且 degree 表只有几行，缓存收益为零——直接查库最安全。

> ⚠ **keyword 处理**：`CollegeService/MajorService/ClassService` 的 `list` 带模糊搜索参数，缓存时必须**只在 keyword 为空时命中缓存**（key 用 `scau:cache:dim:{table}:all`），否则搜索结果会被"全量"缓存污染。

### 4.5 缓存失效（evict）埋点清单 —— 关键，漏一个就脏数据

原则：

1. 只要写入/更新/删除 `admission_fact`、`graduation_fact`、`student_fact`、`ocr_log_dim`、`quality_score_dim`，或改变 `storage/archive` 文件集合，都必须 `evictDashboard()`。
2. 只要写入/更新/删除被缓存的维度表，则同时 evict 对应 dim key。
3. TTL（Dashboard 5 分钟 / 维度 30 分钟）只是兜底，不能代替主动 evict；漏埋点会导致短暂脏数据。

| 写入口 | 位置 | evict 内容 |
|---|---|---|
| 文件上传事实数据入库 | `DataPersistenceService.saveAdmissionData` / `saveGraduationData`，包括 `student_fact` 同步更新 | dashboard |
| 文件成功归档（影响 `DashboardService.totalFiles`） | `StorageService.moveArchiveFile` | dashboard |
| 文件失败归档（影响后续日志同步与处理状态） | `StorageService.failedFile`；若同时写入/同步 `ocr_log_dim`，也必须 evict dashboard | dashboard |
| 维度表自动创建（OCR/LLM 入库时） | `DataPersistenceService.fuzzyLookupMajor/Class/Destination`、`ensureDefaultCollege` | 对应 dim + dashboard |
| 学历/学位自动创建 | `DataPersistenceService.fuzzyLookupDegree` | dashboard（degree 不缓存 dim，但 `degreeDistribution` 依赖 degree 名称） |
| OCR 日志（影响 todayUploads） | `OCRLogService.syncTodayLogs`（**独立入口**，扫描归档目录补日志，不伴随事实表写入）、`addLog`、`removeById` | dashboard |
| 质量评分（影响 avgQuality） | `QualityScoreService.scoreFile` | dashboard |
| 学籍/招生/毕业 CRUD | `StudentService.add/update/delete`、`AdmissionService.add/update/delete`、`GraduationService.add/update/delete` | dashboard |
| 学籍自由输入自动建专业/班级 | `StudentService.resolveMajorId` / `resolveClassId` / `ensureDefaultMajor` / `ensureDefaultCollege` | major/class/college dim |
| 维度管理 CRUD | `CollegeService.add/update/delete`、`MajorService.add/update/delete`、`ClassService.add/update/delete` | 对应 dim + dashboard（专业数影响 majorCount） |

> 注意 `MajorService.list` / `ClassService.list` 内部还读了 college/degree/major 整表做名称映射，缓存时把映射表一起缓存进同一条 JSON，evict 时一并失效。

> 注意 `DashboardService.totalFiles` 直接遍历 `storage/archive`，不是查询 `archive_file_dim`。因此只在 `DataPersistenceService.saveArchiveFileDimData` 后 evict 不够，必须覆盖 `StorageService.moveArchiveFile` 成功路径。

> 注意 `todayUploads` 依赖 `ocr_log_dim`。目前部分 Processor 只有 warning / failed 场景才显式写 `OCRLogService.addLog`，成功无 warning 时可能依赖 `syncTodayLogs` 补日志；若业务要求“上传成功立即反映今日上传数”，应统一在成功处理后写日志并 evict dashboard。

> **明确不需要 evict 的写入口**（已核对）：`MetaDataService`（metadata_standard，不参与统计）、`UserManageService`/`UserService`（sys_user）、`DataPersistenceService.saveArchiveFileDimData`（archive_file_dim 不参与 Dashboard 统计）、`FieldCorrectionService`/`MetaDataMappingService`（纯内存映射不写库）。

---

## 5. 前端改动（验证码 uuid 方案）

- `src/api/modules/auth.js`：新增 `fetchCaptcha()` → `POST/GET /api/captcha` 拿 `{ uuid, imageBase64 }`（替换现在直接拼 `<img src>` 的方式）；`loginRequest` 载荷增加 `uuid` 字段。
- `src/store/user.js`：`captchaUrl` 改为 `captchaUuid` + `captchaImg`（`data:image/png;base64,...`），`refreshCaptcha()` 调 `fetchCaptcha()`。
- `src/views/login/Login.vue`：`<img :src="captchaImg">`，登录时把 `captchaUuid` 随请求体提交。

---

## 6. docker-compose 部署改动

### 6.1 `docker-compose.yml` 新增 redis 服务

```yaml
  # Redis（验证码/限流/缓存）
  redis:
    image: redis:7-alpine
    container_name: scau-redis
    restart: unless-stopped
    command: ["redis-server", "--appendonly", "no"]
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

`backend` 服务改动：

```yaml
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      # ...原有配置不变，新增：
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-}
```

> **Dockerfile 不需要改**——Redis 是独立进程，不进后端镜像。

### 6.2 环境变量

`.env` / `.env.example` 新增：

```
# Redis（本地开发默认 localhost:6379 无密码，可不填）
REDIS_HOST=redis          # docker 部署填 redis，本地填 localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
```

---

## 7. 验证步骤

1. 启动 Redis（本地任选第 3 节方案，Docker 部署自动拉起）。
2. 启动后端，观察日志无 Redis 连接报错。
3. 浏览器打开登录页 → 抓包确认 `GET /api/captcha` 返回 `{uuid, imageBase64}` → 输入验证码登录成功。
4. 验证限流：连续错 8 次密码 → 第 9 次返回 429。
5. 验证缓存：`redis-cli keys 'scau:*'` 能看到 `scau:cache:dashboard`；`redis-cli monitor` 观察 Dashboard 接口只打一次聚合 SQL，5 分钟内再次访问走缓存。
6. 验证 evict：在数据管理页增删一条记录 → `redis-cli get scau:cache:dashboard` 返回 nil（已失效）。
7. 验证降级：停掉 Redis 再访问 Dashboard → 页面仍正常（直接查库）；登录 → 提示"验证码服务异常"而非崩溃。

---

## 8. 风险与回滚

| 风险 | 说明 | 缓解 |
|---|---|---|
| Redis 短暂不可用 | 登录/验证码不可用（fail-closed）；已登录用户不受影响（走 JWT） | 第 4.3 节降级策略 |
| 缓存一致性 | evict 漏埋点导致短暂旧数据 | TTL 兜底（Dashboard 5min / 维度 30min），最多延迟一个 TTL |
| 误用 Redis | 大文件、pgvector 检索等不适合放 Redis | 见 CLAUDE.md 架构约定，勿扩展范围 |

**回滚**：代码改动集中且可逆，`git revert` 对应提交即可；无需清数据库、无需清存储目录。

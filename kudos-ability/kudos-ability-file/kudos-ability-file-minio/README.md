# kudos-ability-file-minio

`kudos-ability-file-common` 三个 SPI（上传 / 下载 / 删除）的 **MinIO（S3 兼容）** 实现。
单机 / 容器 / 云上对象存储部署的首选。

## 设计要点

### 双客户端模式

每个服务都持有两路 MinIO 客户端：

| 客户端 | 来源 | 用途 |
|---|---|---|
| 静态 | `kudos.ability.file.minio.{endpoint,accessKey,secretKey}` | 默认 `minioClient` bean，全局共享 |
| 动态 | `MinioClientBuilderFactory` 按 [AuthServerParam] 现场构造 | 业务侧 `model.authServerParam` 非空时走这条 |

业务侧传 `model.authServerParam` 时进入动态分支，可按"每用户一份凭证"实现真正的 STS
（多租户场景常见）。空时复用全局静态客户端。

### `MinioClientBuilderFactory` 分发

```
AuthServerParam 子类型 → MinioClientBuilder 实现
─────────────────────────────────────────────────
AccessKeyServerParam   → AccessKeyMinioClientBuilder    (AK/SK 直连)
AccessTokenServerParam → AccessTokenMinioClientBuilder  (OAuth2 token → MinIO STS)
其他                   → null（调用方处理）
```

新增认证形式时：(a) 在 `file-common` 增加 `AuthServerParam` 子类，(b) 在本模块增加
`MinioClientBuilder` 实现，(c) 在工厂的 `when` 加分支。

### OAuth2 STS 简化版（`AccessTokenMinioClientBuilder`）

```
业务侧 token (header value)
   ↓
本模块以 Basic clientId:clientSecret 请求 OAuth2 token endpoint
   ↓
拿到 JWT (access_token + expires_in)
   ↓
喂给 MinIO 的 WebIdentityProvider → MinioClient 凭证
```

**安全修正**：旧实现 `log.info("Minio oauth2 server OIDC token:${jwt.token()}")` 把
access_token 明文写日志——是真实泄漏面。已改为 `log.debug` 只记录 `expires_in`，
不输出 token 字符串。

**性能修正**：旧实现 `OkHttpClient()` 每次调用现 new 一个——重型对象（线程池 + 连接池
+ dispatcher）。已移到 companion `object` 共享单例。`JsonMapper` 同理。

`AccessTokenJwtMapperTest` 锁定了 Jackson 3 mapper 配置对 Minio `Jwt` 的反序列化行为
（关键是 `changeDefaultVisibility { withFieldVisibility(ANY) }` 让 Jackson 能读私有字段）。

### 自动建桶（仅静态客户端模式）

`MinioUploadService.createBucket` 检查 bucket 是否存在，不存在则 `makeBucket`。
**仅静态模式下**——动态认证场景（用户级 token）通常没有 `s3:CreateBucket` 权限，连
`bucketExists` 都可能 403，所以直接跳过假定 bucket 已预创建。

旧实现这里有一段 `setPolicy(...)` 配置匿名读策略的代码，但其 PolicyVersion 写成了
`"2025-07-02"`（标准应当是 AWS IAM 的 `"2012-10-17"`），MinIO/S3 会拒收；另外开匿名
读非所有部署都接受。**已删除**——bucket policy 应通过 MinIO 控制台 / mc 命令显式
配置，避免应用层悄悄放公网读。

### 删除流程（`MinioDeleteService`）

`statObject` 先探测对象存在性（捕获 NoSuchKey / NoSuchBucket / AccessDenied 区分错误码），
再 `removeObject`。`statObject` 的额外 RTT 是为了让 `delete()` 在文件不存在时抛
`FILE_NO_EXISTS`，与 file-local 版语义对齐。

### `endpoint` vs `publicEndpoint`

- `endpoint` —— **内部访问**地址（K8s service DNS / 内网 IP），所有 putObject / getObject 用它
- `publicEndpoint` —— **外网访问**地址（ingress / CDN），作为 `UploadFileResult.pathPrefix` 返回，
  前端直接拼接 `pathPrefix + filePath` 形成可访问 URL

两者通常不同——内部走低延迟链路，外部走经过 WAF / CDN 的链路。

## 配置示例

```yaml
kudos:
  ability:
    file:
      minio:
        endpoint: http://minio.internal:9000     # 内网
        access-key: ${MINIO_ACCESS_KEY}           # 必配：jar 内不再内置默认凭证，缺失则启动快速失败
        secret-key: ${MINIO_SECRET_KEY}           # 必配：同上，建议通过环境变量注入
        public-endpoint: https://cdn.example.com  # 给前端拼 URL
        part-size: 10485760                       # 未知大小流 multipart 分片，默认 10MiB，最小 5MiB
        sts:
          access-token:
            enabled: false                        # 不用 OAuth2 STS 时设为 false
            client-id: minio
            client-secret: ${OAUTH_CLIENT_SECRET}
            authorization-grant-type: client_credentials
            client-authentication-method: client_secret_basic
            endpoint: http://oauth.internal:10001/oauth2/token
            header-name: Authentication-Info
```

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/MinioAutoConfiguration` | 装配入口；7 个 bean（client / properties / 3 个 service / builder factory） |
| `init/properties/MinioProperties` | endpoint / AK / SK / publicEndpoint / partSize |
| `init/properties/AccessTokenServerProperties` | OAuth2 STS 配置 |
| `init/properties/AuthServerProperties` | 抽象基类（占位） |
| `MinioUploadService` / `MinioDownLoadService` / `MinioDeleteService` | 三个 SPI 实现 |
| `client/MinioClientBuilder` | 单认证类型的 builder SPI |
| `client/AccessKeyMinioClientBuilder` | AK/SK 直连 |
| `client/AccessTokenMinioClientBuilder` | OAuth2 token → STS |
| `client/MinioClientBuilderFactory` | 按 AuthServerParam 分发 |

## 测试覆盖

- `MinioUploadServiceTest`（3）/ `MinioDownLoadServiceTest`（3）/ `MinioDeleteServiceTest`（4）
  —— 基于 `MinioTestContainer` 的端到端
- `AccessTokenJwtMapperTest`（2）—— 锁定 Jackson 3 mapper 对 Minio `Jwt` 私有字段的反序列化
  能力，防止 Jackson API 升级悄悄破坏 STS 链路
- `MinioPropertiesTest`（3）—— 锁定上传 multipart 分片大小的默认值、可配置性和最小值校验
- `MinioAutoConfigurationTest`（6）—— 凭证/endpoint 缺失或空白时装配快速失败、
  异常消息含配置项全名（纯单测，不依赖 Docker）
- `MinioUploadServiceResolveFileNameTest`（7）—— `fileName` 含 `..` / `/` / `\` 被拒、
  空白时回退 UUID 命名（纯单测，不依赖 Docker）

端到端测试依赖 Docker 跑 testcontainer，单测无需。

## 已知限制 / 后续工作

- ❗ `MinioDeleteService.isValid` 继承自 common 的字符串包含 `..` 检查——**对象存储里
  "filePath" 是 S3 key（任意字符串），不是文件系统路径**，不存在真正的"跳出 base-path"
  语义，该检查在这里更多是兼容形式。如果业务需要严格限制 key 字符集，应自行加白名单
- ✅ `MinioUploadService.saveFile` 的 multipart part size 已改为
  `kudos.ability.file.minio.part-size` 配置，默认 10MiB，并在属性层拒绝低于 MinIO/S3
  最小 5MiB 的配置值
- ❗ 动态客户端模式（`authServerParam != null`）每次调用都 build 新 MinioClient——MinioClient
  内部连接池随之新建。高并发动态场景可能成性能瓶颈，需要时可加按 (endpoint, principal) 维度
  的客户端缓存
- ❗ `AccessTokenMinioClientBuilder.accessToken` 每次都拿 token，没缓存——OAuth2 token 一般
  有 expires_in（默认 1h），缓存到接近过期前可大幅减少 OAuth2 服务器压力
- ❗ `AuthServerProperties` 是个空抽象基类，仅 `AccessTokenServerProperties` 一个子类；
  暂时算占位，未来若有其他认证服务器类型再启用
- ❗ `RetentionRetentionMode` 之类的 MinIO 高级特性（对象版本 / WORM / 加密策略）未对外
  暴露——业务侧若需要这些需直接持有 `MinioClient` bean 自行调用

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-file:kudos-ability-file-common"))
api(libs.minio)
implementation(libs.jackson.databind)

testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(libs.minio.admin)
```

`jackson.databind` 是 `implementation` 不是 `api`——内部用 Jackson 3 反序列化 Minio
`Jwt`，但不希望传递给业务侧（业务侧应该用 kudos 提供的 JSON 工具）。

## 改进建议（自动分析 2026-06-11）

本次已直接修复：

- ✅ `MinioUploadService.kt`：`saveFile` 中 `inputStreamSource` 打开的输入流从不关闭
  （file-local 实现用了 `.use {}`，本模块漏了）——已改为 `inputStream.use { ... }` 包裹
  压缩 + putObject 全程，上传结束即释放。
- ✅ `client/AccessTokenMinioClientBuilder.kt`：OAuth2 token 端点响应未检查
  `response.isSuccessful`——401/503 返回 HTML 时会以一个难以理解的 JSON 反序列化异常暴露。
  已改为非 2xx 直接抛 `ProviderException`（刻意不带响应体，防止错误页回显业务 token 入日志）。
- ✅ 已修复（2026-06-11）`resources/kudos-ability-file-minio.yml`：**内置默认凭证
  `admin/12345678`** 已删除（yml 仅留注释示例），并在 `MinioAutoConfiguration.minioClient`
  装配处对 endpoint / access-key / secret-key 空值快速失败，异常消息直接给出配置项全名。
  三个容器级测试本就通过 `@DynamicPropertySource` 显式注入测试用户凭证，测试链路不受影响。
- ✅ 已修复（2026-06-11）`MinioUploadService.saveFile`：补上与 `LocalUploadService` 一致的
  `fileName` basename 校验（`resolveFileName`），含 `..` / `/` / `\` 的文件名直接抛
  `FILE_UPLOAD_FAIL`，防止 `publicEndpoint + filePath` 拼 URL 时 `..` 段被浏览器/网关/CDN
  归一化指向其他对象。**行为变化**：之前通过 `fileName` 传"带斜杠嵌套 key"的用法会被拒绝
  （目录部分始终由 `dispatchFileDir` 生成，不受影响）。

待办（按维度分类）：

### 1. 错误语义

- `MinioDownLoadService.kt`：把所有 `ErrorResponseException` 一律映射为
  `FILE_ACCESS_DENY`——`NoSuchKey`/`NoSuchBucket` 应映射 `FILE_NO_EXISTS`
  （`MinioDeleteService` 已经按 code 细分了，下载侧不一致）。属调用方可见的错误码变化，
  未直接改。

### 2. 可维护性

- `getMinioClient(...)` 在 3 个服务类中逐字重复（仅 model 类型不同），建议抽公共基类或组合
  一个 `MinioClientResolver(authServerParam?) : MinioClient` 组件，三处只留一行。
- `MinioUploadService` / `LocalUploadService` 的
  `"${RandomStringKit.uuid()}.${model.fileSuffix}"`：`fileSuffix` 为 null 时生成
  `xxx.null`、传 `".txt"` 时生成 `xxx..txt`，建议规范化（去前导点、null 则不拼后缀）。

### 3. 可观测性

- `getMinioClient` 每次请求 `LOG.info` 认证类型——高流量下纯噪音，建议降为 debug。
- 上传/下载无耗时与字节数指标；动态客户端构建（含一次 OAuth2 网络往返）也无耗时记录，
  建议在 STS 链路加 Timer，token 缓存命中率未来可一并暴露。

### 4. 测试缺口

- `AccessKeyMinioClientBuilder` / `MinioClientBuilderFactory` 无直接单测（工厂分发逻辑是纯函数，
  成本极低）；OAuth2 STS 动态认证链路（`AccessTokenMinioClientBuilder.build` → MinIO）只有
  mapper 反序列化测试，无 wiremock/容器级集成测试，`enabled=false` 之外的配置组合全靠生产验证。

### 5. 功能补充

- 预签名 URL：MinIO SDK 的 `getPresignedObjectUrl` 一行可得，是"前端直传/临时授权下载"的
  核心能力，建议经 file-common 的新 SPI 暴露（见 file-common README 同日建议）。
- 分片上传/断点续传：SDK 内部已按 `partSize` 分片，但未对外暴露"恢复上传"语义；如有大文件
  断点需求，需要暴露 uploadId 级 API 或改走预签名分片直传。

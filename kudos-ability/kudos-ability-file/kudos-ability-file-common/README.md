# kudos-ability-file-common

文件上传 / 下载 / 删除 / 图像压缩的"共享层"。给具体存储后端（本地磁盘 / MinIO / S3 / OSS）
提供统一 SPI，让业务侧 `@Autowired IUploadService` 而不在意底下是哪种存储。

## 设计要点

### 三个核心 SPI

| 接口 | 入参 | 用途 |
|---|---|---|
| `IUploadService` | `UploadFileModel<S>` | 上传 → `UploadFileResult`（filePath + pathPrefix） |
| `IDownLoadService` | `DownloadFileModel<S>` | 下载 → `ByteArray?` 或 `InputStream?`（按大文件 / 小文件分） |
| `IDeleteService` | `DeleteFileModel` | 删除 → Boolean |

业务侧调用 `@Autowired` 直接拿；具体实现由 `file-local` / `file-minio` 等子模块提供。

### 上传路径分配规则（`AbstractUploadService.dispatchFileDir`）

```
{pathPrefix}/{bucket}/{tenantId?}/{category?或 yyyy/MM/dd}/{uuid}.{ext}
```

- `tenantId` 非空时优先拼到路径里——上传天然按租户隔离
- `category` 非空走分类目录；否则按"年/月/日"自动分级（避免单目录文件爆炸）
- 路径分隔符固定用 `/`（跨 Windows + Unix + Web URL 都兼容）

### 路径解析的"必须前导 `/`"约定

`DeleteFileModel.from(fullPath)` / `DownloadFileModel.from(fullPath)` 解析形如
`/<bucket>/<file/path>` 的完整路径。**修复前的 bug**：旧实现按 `/` 切分后用
`segments[1]` 当 bucket，依赖输入有前导 `/`（这样 segments[0] 是空、segments[1] 是 bucket）。
但代码没强制检查——无前导 `/` 时返回错误的 bucket（首段会被当 `segments[0]` 即"空"，
反而 `segments[1]` 是真正第一段下面的，错位一格）。

**现在**：显式 `require(fullPath.startsWith("/"))`，错路径直接抛 `IllegalArgumentException`
而不是默默给出错位结果。

### 路径穿越检查的修正（`IDeleteService.isValid`）

旧实现：
```kotlin
val relativePath = model.bucketName + File.pathSeparator + model.filePath
```

`File.pathSeparator` 是**环境变量 `PATH` 的分隔符**（Unix `:` / Windows `;`），不是
文件路径片段之间的分隔符（`File.separator`：Unix `/` / Windows `\`）。拼出来的串形如
`bucket:filePath`，跟实际即将被使用的 `bucket/filePath` 是两个东西，使得 `..` 检查
**检的根本不是真路径**——一定意义上是 no-op。已修。

注意：本检查仍很粗粒度（仅查字符串包含 `..`）。生产部署的具体后端实现 `delete`
内部还应做 `Path.normalize() + startsWith(rootDir)` 等强校验。

### MIME 类型映射的 BMP/PDF 冲突修复

`UploadContentTypeEnum` 旧实现：
```kotlin
BMP("pdf", "application/x-bmp"),  // suffix 写成了 "pdf"
PDF("pdf", "application/pdf"),
```

后果：
- `enumOf("bmp")` → DEFAULT（找不到匹配）
- `enumOf("pdf")` → BMP（顺序匹配命中错的那个）→ PDF 文件被打上 `application/x-bmp`

**已修**：`BMP("bmp", "image/bmp")`，并把 `for` 循环写法改成 `entries.firstOrNull`。

### 图像压缩管线

```
CompressionPipeline.compress(InputStream, outputFilePath, CompressionConfig)
  ↓
isPic(outputFilePath) ? ImageCompressorFactory.getCompressor(...) : skip
  ↓
JpgCompressor      (image/jpeg)
PngCompressor      (image/png, 委托 com.xqlee.image.png.PngCompressor)
WebPCompressor     (image/webp, config.webp=true 时强制使用)
```

`CompressionConfig`：`enabled` / `webp` / `width` / `height` / `quality (0.0~1.0)`。

WebP 模式下生成的文件名会被加上 `.webp` 后缀（即便原文件已经是 webp）。

### `CompressionResult.writeTo` 的错误吞掉历史

旧实现 `catch (ignored: Exception) {}` 默默吞掉所有 IO 错误——压缩失败时调用方完全
不知道。**已修**：log error 让事故可见。仍不向上抛是为了不打破
`CompressionPipeline.compressAndOutputFile` 的现有签名（不声明 IOException），后续可考虑改抛。

## 认证参数

| 类 | 用途 |
|---|---|
| `AuthServerParam` | 空接口（标记 + 多态分发） |
| `AccessKeyServerParam` | AK / SK 双串（MinIO / S3 / OSS） |
| `AccessTokenServerParam` | 单 token / Bearer header |

## 模块入口

| 路径 | 角色 |
|---|---|
| `IUploadService` / `IDownLoadService` / `IDeleteService` | 三个核心 SPI |
| `AbstractUploadService` / `AbstractDownLoadService` | 默认模板基类（路径分配 + 错误透传） |
| `entity/UploadFileModel` / `DownloadFileModel` / `DeleteFileModel` / `UploadFileResult` | 请求 / 响应 POJO |
| `auth/AuthServerParam` 等 3 个 | 认证参数 |
| `enums/UploadContentTypeEnum` | 后缀 → MIME |
| `code/FileErrorCode` | 错误码字典 |
| `compress/CompressionPipeline` | 压缩入口 |
| `compress/compressor/*` | 4 个具体压缩器（Image / Jpg / Png / WebP） |
| `compress/support/*` | 压缩配置 + 结果 + 工厂 |
| `compress/utils/CompressUtil` | 后缀白名单 + MIME 探测 |

## 测试覆盖

- `UploadContentTypeEnumTest`（4 case）—— 锁定 BMP/PDF 修复后的行为
- `DeleteFileModelTest`（5 case）—— 锁定前导 `/` 约束和错误路径拒绝

具体存储后端的端到端测试在 `file-local` / `file-minio` 子模块内。

## 已知限制 / 后续工作

- ❗ `IDeleteService.isValid` 的 `..` 检查仍是粗粒度字符串包含；URL 编码变种 / 软链
  穿越 / 不规范化路径都不防——具体后端应自己再做 `Path.normalize + startsWith(root)` 强校验
- ❗ `AbstractUploadService.dispatchFileDir` 使用 JVM 默认时区取 `LocalDate.now()`——
  跨时区部署 / 容器时区不一致时上传路径日期可能与日志不对齐。可考虑接 `Clock`
- ❗ `UploadFileResult.filePath` / `pathPrefix` 都是 `var String?`，POJO 风格；新代码
  建议改成 `data class` + 必填字段非空，但要兼容现有反序列化
- ❗ `CompressionResult.writeTo()` 把 IO 错误 log + 吞，不向上抛——是为了兼容现有调用方
  签名（`CompressionPipeline.compressAndOutputFile` 不声明 IOException），但下游可能
  在"以为压缩成功了"的前提下继续推进，需要时改抛
- ❗ `WebPCompressor` 强制走有损模式（默认 quality 0.75），`CompressionConfig.quality`
  仅对 Jpg 生效；webp 模式下 quality 字段实际上被忽略，文档未说明
- ❗ `ImageCompressorFactory.getCompressor(outputFilePath, webp)` 用
  `Files.probeContentType` 探测——文件还**没存**时这个 API 在某些 JDK 上靠后缀判定，
  跨平台行为不一致。建议显式按后缀分发
- ❗ 没有"先存大小限制 / MIME 白名单 / 病毒扫描"的钩子；安全敏感场景上传需自行加 Aspect

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.sejda.webp.imageio)
api(libs.coobird.thumbnailator)
api(libs.xqlee.pngquant.png)

testImplementation(project(":kudos-test:kudos-test-common"))
```

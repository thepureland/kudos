# kudos-ability-file-local

`kudos-ability-file-common` 三个 SPI（上传 / 下载 / 删除）的**本地磁盘**实现。开发期和
单机部署使用；正式生产场景应当切换到 `file-minio` 等远程对象存储。

## 设计要点

### 配置

```yaml
kudos:
  ability:
    file:
      local:
        base-path: ${user.home}/fserver/upload   # 默认值，生产请改到挂载点
```

`base-path` 是所有文件的根目录；上传时 `{base-path}/{bucket}/...`，下载 / 删除同样从这里
拼路径。

### 路径穿越防御（重要）

`file-common` 的 `IDeleteService.isValid` 只做粗粒度的字符串包含 `..` 检查，**不够**。
本模块额外加一层：

```kotlin
val baseDir = File(base).toPath().toAbsolutePath().normalize()
val resolved = File(rawPath).toPath().toAbsolutePath().normalize()
if (!resolved.startsWith(baseDir)) {
    // 路径解析后跳出 base-path → 拒绝
}
```

`LocalDeleteService` 在删除前做这层检查，命中穿越企图直接 `return false` + warn 日志，
不真正落到 `file.delete()`。

`LocalDownLoadService` 也做了同样的 `resolveSafePath` 检查；命中时抛
`ServiceException(FileErrorCode.FILE_ACCESS_DENY)`。

`LocalUploadService` 当前不做穿越检查——上传路径由框架内部 `dispatchFileDir` 生成
（含 uuid / 日期），不来自用户输入；如果业务侧自定义 `fileName` 是用户可控字符串，
需要在调用前做白名单。

### 三个服务的职责

| Bean | 职责 |
|---|---|
| `LocalUploadService` | 创建 bucket / fileDir 目录 + 走 `CompressionPipeline.compressAndOutputFile` 落盘 |
| `LocalDownLoadService` | `readFileToByte` 整文件读到内存（小文件） / `readFileToStream` 流式（大文件） |
| `LocalDeleteService` | 双重穿越检查 + 拒绝删目录 + 文件不存在抛 `FILE_NO_EXISTS` |

### 上传返回的 filePath 形态

```
"/<bucket>/<tenantId?>/<category? or yyyy/M/d>/<uuid>.<ext>"
```

以 `/` 开头是有意的——和 `DeleteFileModel.from / DownloadFileModel.from` 要求的"必须前导
`/`"约定对齐。

`UploadFileResult.pathPrefix` 在本模块下返回 `""`（本地路径不对外暴露），所以业务侧
看到的是相对路径；如果上传后需要直接通过 URL 访问，需要在 Web 层挂个 controller 把
`base-path` 当静态资源暴露——本模块不提供这层。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/FileLocalAutoConfiguration` | 装配入口（4 个 bean） |
| `init/LocalUploadService` | 上传实现 |
| `init/LocalDownLoadService` | 下载实现（含穿越检查） |
| `init/LocalDeleteService` | 删除实现（含穿越检查） |
| `init/properties/LocalProperties` | base-path 配置 |

## 测试覆盖

- `LocalUploadServiceTest`（2 case）—— 带分类目录上传 + 不带分类按日期上传
- `LocalDeleteServiceTest`（4 case）—— `..` 路径拒绝、目录拒绝、不存在抛错、正常删除；
  每个用例使用独立 `@TempDir` 作为 `basePath`
- `LocalUploadServiceTest`（3 case）—— 分类 / 按日期目录写入、拒绝 `../` 形式的文件名穿越；
  每个用例使用独立 `@TempDir` 作为 `basePath`

依赖 kudos-test-common；不需要 Docker。

## 已知限制 / 后续工作

- ✅ `LocalUploadService` 已对显式 `model.fileName` 做 basename 校验，拒绝 `..`、`/`、
  `\`，并对最终路径做 `normalize + startsWith(basePath)` 校验
- ✅ local 模块测试已改用 `@TempDir` 覆盖 `LocalProperties.basePath`，不再写入
  `${user.home}/fserver/upload`
- ❗ `pathPrefix()` 返回空串，业务侧无法直接通过 HTTP URL 访问上传后的文件——需要
  Web 层挂载静态资源 mapping。本模块不提供（与 file-minio 直接返回 URL 的设计不一致）
- ✅ 已移除 `LocalUploadService.createBucket` 冗余调用；`createFileDir` 会一并创建 bucket 和中间目录
- ❗ 单文件大小没限制；上传超大文件可能耗尽 IO / 磁盘。生产侧应在网关 / 上传接口加上限
- ❗ 文件落盘没有 sync/fsync 调用——`File.createNewFile` 后断电可能丢内容（kernel pagecache）。
  本地存储一致性要求不高的场景可以接受

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-file:kudos-ability-file-common"))

testImplementation(project(":kudos-test:kudos-test-common"))
```

## 改进建议（自动分析 2026-06-11）

### 1. 可维护性 / Kotlin 写法

- `init/LocalDeleteService.kt`、`init/LocalDownLoadService.kt`：
  `listOf(base, model.bucketName, model.filePath).joinToString(File.separator)` 在
  bucketName/filePath 为 null 时会把字面量 `"null"` 拼进路径（`List<String?>` 的
  joinToString 对 null 渲染为 "null"），最终虽落到 `FILE_NO_EXISTS`，但校验与报错信息
  失真。建议入口处显式 `requireNotNull` 并给出明确参数名。
- 上述两个类的 `normalize + startsWith(baseDir)` 穿越校验是重复实现（一个 return false、
  一个抛 `FILE_ACCESS_DENY`），建议抽 internal 工具函数统一维护，避免日后只改一处。
- `init/LocalUploadService.kt` 的 `createFileDir` 未检查 `mkdirs()` 返回值；并发创建/权限
  不足时静默失败，错误延迟到写文件时才暴露。建议改用 `Files.createDirectories`（失败即抛）。

### 2. 测试缺口（安全关键）

- **没有 `LocalDownLoadServiceTest`**——`resolveSafePath` 的路径穿越防护
  （`FILE_ACCESS_DENY`）与正常下载（byte/stream 两条路）完全无测试覆盖。删除侧、上传侧
  都有穿越用例，下载侧是三者中最常暴露给用户输入的，建议优先补：
  正常读取 / `../` 穿越拒绝 / 文件不存在抛 `FILE_NO_EXISTS`。

### 3. 可观测性

- 上传成功后无任何 info 级日志（只有 debug 的目录创建）；建议在 saveFile 成功后记一条
  debug/info 含相对路径与字节数，便于排查"文件去哪了"。

### 4. 功能 / 一致性

- `pathPrefix()` 返回空串导致与 file-minio 返回完整 URL 的行为不一致（已知限制中已述）。
  若要统一，可加 `kudos.ability.file.local.public-url-prefix` 配置项，默认空串保持兼容。

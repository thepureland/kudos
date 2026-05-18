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
- `LocalDeleteServiceTest`（4 case）—— `..` 路径拒绝、目录拒绝、不存在抛错、正常删除

依赖 kudos-test-common；不需要 Docker。

## 已知限制 / 后续工作

- ❗ `LocalUploadService` 自身不做路径穿越检查——假设 `fileName` 来自框架内部 uuid。
  如果业务侧用用户输入做 `model.fileName`，需自行加白名单
- ❗ 测试在 `${user.home}/fserver/upload/__/__*` 创建数据但**不清理**；连续跑测试会
  累积，且与生产 `base-path` 共享根目录时风险更大。建议测试用 `@TempDir`
- ❗ `pathPrefix()` 返回空串，业务侧无法直接通过 HTTP URL 访问上传后的文件——需要
  Web 层挂载静态资源 mapping。本模块不提供（与 file-minio 直接返回 URL 的设计不一致）
- ❗ `LocalUploadService.createBucket` 是冗余调用——`createFileDir` 会一并创建中间目录
  含 bucket。可移除
- ❗ 单文件大小没限制；上传超大文件可能耗尽 IO / 磁盘。生产侧应在网关 / 上传接口加上限
- ❗ 文件落盘没有 sync/fsync 调用——`File.createNewFile` 后断电可能丢内容（kernel pagecache）。
  本地存储一致性要求不高的场景可以接受

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-file:kudos-ability-file-common"))

testImplementation(project(":kudos-test:kudos-test-common"))
```

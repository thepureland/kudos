# kudos-ability-file

文件存储能力主题——上传 / 下载 / 删除 + 图像压缩。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-file-common`](kudos-ability-file-common/README.md) | SPI（`IUploadService` / `IDownLoadService` / `IDeleteService`）+ 图像压缩管线 |
| [`kudos-ability-file-local`](kudos-ability-file-local/README.md) | 本地磁盘实现（开发期用） |
| [`kudos-ability-file-minio`](kudos-ability-file-minio/README.md) | MinIO / S3 兼容（生产用） |

业务侧 `@Autowired IUploadService` 即可，不必区分底层存储。

## 改进建议（自动分析 2026-06-11）

本次深度审查的逐条结论已按子模块落到各自 README 的同名章节：

- [`kudos-ability-file-common/README.md`](kudos-ability-file-common/README.md)——已修复
  JpgCompressor 两个真实 bug（默认配置 `size(0,0)` 必崩、`quality` 从未生效）并补回归测试；
  待办含 MIME 表修正、magic-bytes 校验钩子、预签名/分片 SPI、`from()` 重复代码等
- [`kudos-ability-file-local/README.md`](kudos-ability-file-local/README.md)——待办含
  **下载侧穿越防护无测试**、null 拼出 `"null"` 路径段、穿越校验重复实现等
- [`kudos-ability-file-minio/README.md`](kudos-ability-file-minio/README.md)——已修复上传流
  未关闭、OAuth2 响应未查状态码、**yml 内置默认凭证 admin/12345678**（已删除并在装配处
  对凭证缺失快速失败）与 fileName 未校验进入 object key（已补 basename 校验）；
  待办含下载错误码不区分 NoSuchKey、预签名 URL 缺失等

跨模块共性（建议统一处理）：上传/下载无耗时与失败率埋点（`AbstractUploadService` /
`AbstractDownLoadService` 模板方法是统一埋点收口点）；文件大小上限与文件类型白名单
目前完全依赖业务侧自觉，建议下沉为 SPI 层可配置钩子。

# kudos-ability-file

文件存储能力主题——上传 / 下载 / 删除 + 图像压缩。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-file-common`](kudos-ability-file-common/README.md) | SPI（`IUploadService` / `IDownLoadService` / `IDeleteService`）+ 图像压缩管线 |
| [`kudos-ability-file-local`](kudos-ability-file-local/README.md) | 本地磁盘实现（开发期用） |
| [`kudos-ability-file-minio`](kudos-ability-file-minio/README.md) | MinIO / S3 兼容（生产用） |

业务侧 `@Autowired IUploadService` 即可，不必区分底层存储。

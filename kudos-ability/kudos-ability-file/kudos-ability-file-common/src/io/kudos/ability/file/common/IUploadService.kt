package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult

/**
 * 文件上传服务 SPI。具体存储后端（本地磁盘 / MinIO / OSS）各自实现。
 * 业务侧通过 `@Autowired IUploadService` 注入，框架按当前应用引入的子模块决定具体实现。
 *
 * @author K
 * @since 1.0.0
 */
interface IUploadService {
    /**
     * 上传文件
     *
     * @param model m
     * @return r
     */
    fun fileUpload(model: UploadFileModel<*>): UploadFileResult

    /**
     * 获取文件路径前缀：一般minio返回对应的url地址
     */
    fun pathPrefix(): String
}
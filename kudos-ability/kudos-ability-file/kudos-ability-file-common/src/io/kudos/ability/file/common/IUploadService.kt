package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult

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
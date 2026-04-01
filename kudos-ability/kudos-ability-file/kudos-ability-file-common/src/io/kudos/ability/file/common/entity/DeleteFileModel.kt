package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import java.io.Serializable

class DeleteFileModel : Serializable {
    /**
     * 自定义目录|存储空间名称
     */
    var bucketName: String? = null

    /**
     * 完整文件路径
     */
    var filePath: String? = null

    /**
     * 认证参数
     */
    var authServerParam: AuthServerParam? = null

    companion object {
        fun from(fullPath: String): DeleteFileModel {
            require(fullPath.isNotBlank()) { "fullPath must not be blank" }
            val segments = fullPath.split('/').dropLastWhile { it.isEmpty() }
            val bucketName = segments.getOrNull(1)
                ?: throw IllegalArgumentException("fullPath must contain bucket segment: $fullPath")
            val filePath = fullPath.removePrefix("/$bucketName")
            return DeleteFileModel().apply {
                this.bucketName = bucketName
                this.filePath = filePath
            }
        }
    }
}

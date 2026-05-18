package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import org.springframework.core.io.InputStreamSource
import java.io.Serial
import java.io.Serializable

class DownloadFileModel<S : InputStreamSource> : Serializable {
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
        @Serial
        private val serialVersionUID = -8498350660950356072L

        /**
         * 从形如 `/<bucket>/<file/path>` 的完整路径拆出 [bucketName] / [filePath]。
         *
         * **要求 `fullPath` 以 `/` 开头**——见 [DeleteFileModel.from] 同款约束。
         */
        fun from(fullPath: String): DownloadFileModel<*> {
            require(fullPath.isNotBlank()) { "fullPath must not be blank" }
            require(fullPath.startsWith("/")) { "fullPath must start with '/': $fullPath" }
            val segments = fullPath.split('/').dropLastWhile { it.isEmpty() }
            val bucketName = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("fullPath must contain bucket segment: $fullPath")
            val filePath = fullPath.removePrefix("/$bucketName")
            return DownloadFileModel<InputStreamSource>().apply {
                this.bucketName = bucketName
                this.filePath = filePath
            }
        }
    }

}

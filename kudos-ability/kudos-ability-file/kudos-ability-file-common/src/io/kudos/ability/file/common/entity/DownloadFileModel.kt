package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import org.springframework.core.io.InputStreamSource
import java.io.Serial
import java.io.Serializable

/**
 * 文件下载请求模型。
 *
 * 与 [DeleteFileModel] 结构一致；额外的泛型参数 `S` 是为了让上传/下载共用同套 [InputStreamSource] 约束。
 *
 * @param S 输入流类型，默认是 [InputStreamSource]
 * @author K
 * @since 1.0.0
 */
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
        /** Serializable 版本号 */
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

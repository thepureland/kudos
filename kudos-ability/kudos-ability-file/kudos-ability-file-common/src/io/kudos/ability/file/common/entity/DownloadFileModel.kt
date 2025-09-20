package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import org.springframework.core.io.InputStreamSource
import java.io.Serial
import java.io.Serializable
import java.util.*

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

        fun from(fullPath: String): DownloadFileModel<*> {
            Objects.nonNull(fullPath)
            val split: Array<String?> = fullPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val bucketName = split[1]
            val filePath = fullPath.replaceFirst(("/$bucketName").toRegex(), "")
            val downloadFileModel = DownloadFileModel<InputStreamSource>()
            downloadFileModel.bucketName = bucketName
            downloadFileModel.filePath = filePath
            return downloadFileModel
        }
    }

}

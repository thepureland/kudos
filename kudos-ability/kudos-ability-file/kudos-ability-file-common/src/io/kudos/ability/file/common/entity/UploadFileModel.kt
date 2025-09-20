package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import io.kudos.ability.file.common.compress.support.CompressionConfig
import org.springframework.core.io.InputStreamSource
import java.io.Serial
import java.io.Serializable

/**
 * 文件上传的model对象
 * 此文件会放在：{系统规划的路径}/{bucket}/{tenantId}/{catePath?}/{yyyy}/{mm}/{dd}/{uuid}.{fileSuffix}
 */
class UploadFileModel<S : InputStreamSource?> : Serializable {
    /**
     * 自定义目录|存储空间名称
     */
    var bucketName: String? = null

    /**
     * 租户id
     */
    var tenantId: String? = null

    /**
     * 分类目录
     */
    var category: String? = null

    /**
     * 文件后缀
     */
    var fileSuffix: String? = null

    /**
     * 输入流
     */
    var inputStreamSource: S? = null

    /**
     * 认证参数
     */
    var authServerParam: AuthServerParam? = null

    /**
     * 文件名, example: test.jpg
     */
    var fileName: String? = null

    var compressionConfig = CompressionConfig()

    companion object {
        @Serial
        private val serialVersionUID = -8498350660950356072L
    }

}

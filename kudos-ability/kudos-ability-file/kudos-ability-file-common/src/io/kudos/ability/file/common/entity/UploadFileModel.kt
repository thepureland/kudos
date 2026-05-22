package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import io.kudos.ability.file.common.compress.support.CompressionConfig
import org.springframework.core.io.InputStreamSource
import java.io.Serial
import java.io.Serializable

/**
 * 文件上传的 model 对象。
 *
 * 最终落盘路径形态：`{系统规划的路径}/{bucket}/{tenantId}/{catePath?}/{yyyy}/{mm}/{dd}/{uuid}.{fileSuffix}`
 * 实际目录由 [io.kudos.ability.file.common.AbstractUploadService.dispatchFileDir] 拼装，本类只承载入参。
 *
 * @param S 输入流类型；保留泛型让上层（如 Spring MultipartFile）能精确传入而无需 cast
 * @author K
 * @author AI: Codex
 * @since 1.0.0
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

    /** 图片压缩配置，留默认值表示不压缩 */
    var compressionConfig = CompressionConfig()

    companion object {
        /** Serializable 版本号 */
        @Serial
        private val serialVersionUID = -8498350660950356072L
    }

}

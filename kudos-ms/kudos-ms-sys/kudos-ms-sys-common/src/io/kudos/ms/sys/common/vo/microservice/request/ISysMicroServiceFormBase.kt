package io.kudos.ms.sys.common.vo.microservice.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank

/**
 * 微服务表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysMicroServiceFormBase {

    /** 编码 */
    @get:NotBlank
    val code: String

    /** 名称 */
    @get:NotBlank
    val name: String

    /** 上下文 */
    @get:NotBlank
    val context: String

    /** 是否为原子服务 */
    val atomicService: Boolean

    /** 父服务编码 */
    val parentCode: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}

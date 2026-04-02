package io.kudos.ms.sys.common.vo.microservice.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * 微服务表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysMicroServiceFormBase {

    /** 编码 */
    @get:NotBlank
    @get:MaxLength(32)
    @get:Matches(RegExpEnum.VAR_NAME)
    val code: String

    /** 名称 */
    @get:NotBlank
    @get:MaxLength(128)
    val name: String

    /** 上下文 */
    @get:NotBlank
    @get:Matches(RegExpEnum.CONTEXT)
    val context: String

    /** 是否为原子服务 */
    val atomicService: Boolean

    /** 父服务编码 */
    @get:MaxLength(32)
    @get:Matches(RegExpEnum.VAR_NAME)
    val parentCode: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}

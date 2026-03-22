package io.kudos.ms.sys.common.vo.param.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 参数表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 参数名称 */
    @get:NotBlank
    val paramName: String = "",

    /** 参数值 */
    @get:NotBlank
    val paramValue: String = "",

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String = "",

    /** 序号 */
    val orderNum: Int? = null,

    /** 备注 */
    @get:MaxLength(128)
    val remark: String? = null,

) : IIdEntity<String?>
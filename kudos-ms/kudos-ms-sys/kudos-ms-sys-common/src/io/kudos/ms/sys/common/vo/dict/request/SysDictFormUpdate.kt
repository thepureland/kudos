package io.kudos.ms.sys.common.vo.dict.request

import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 字典表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 字典类型 */
    @get:NotBlank
    val dictType: String = "",

    /** 字典名称 */
    @get:NotBlank
    val dictName: String = "",

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String = "",

    /** 备注 */
    val remark: String? = null,

) : IIdEntity<String?>
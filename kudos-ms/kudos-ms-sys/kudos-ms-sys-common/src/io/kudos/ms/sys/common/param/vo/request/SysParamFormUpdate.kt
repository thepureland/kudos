package io.kudos.ms.sys.common.param.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * Param update form request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamFormUpdate (

    /** Primary key */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val paramName: String,

    override val paramValue: String,

    override val defaultValue: String?,

    override val atomicServiceCode: String,

    override val orderNum: Int?,

    override val remark: String?,

) : IIdEntity<String>, ISysParamFormBase

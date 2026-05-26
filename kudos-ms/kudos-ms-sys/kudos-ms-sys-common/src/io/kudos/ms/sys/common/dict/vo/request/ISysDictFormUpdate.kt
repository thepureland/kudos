package io.kudos.ms.sys.common.dict.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * Dictionary form update request VO
 *
 * @author K
 * @since 1.0.0
 */
data class ISysDictFormUpdate (

    /** Primary key */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String?,

    override val dictType: String,

    override val dictName: String,

    override val atomicServiceCode: String,

    override val remark: String?,

) : IIdEntity<String?>, ISysDictFormBase

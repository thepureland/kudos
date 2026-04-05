package io.kudos.ms.sys.common.dict.vo.request
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 字典项表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemFormUpdate (

    /** 主键 */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val itemCode: String,

    override val itemName: String,

    override val dictId: String,

    override val orderNum: Int?,

    override val parentId: String?,

    override val remark: String?,

) : IIdEntity<String>, ISysDictItemFormBase

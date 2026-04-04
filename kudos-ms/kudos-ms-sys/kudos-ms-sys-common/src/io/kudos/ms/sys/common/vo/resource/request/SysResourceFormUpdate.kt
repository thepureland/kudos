package io.kudos.ms.sys.common.vo.resource.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 资源表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceFormUpdate (

    /** 主键 */
    @get:NotBlank
    @get:FixedLength(36)
    override val id: String,

    override val name: String,

    override val url: String?,

    override val resourceTypeDictCode: String,

    override val parentId: String?,

    override val orderNum: Int?,

    override val icon: String?,

    override val subSystemCode: String,

    override val remark: String?,

) : IIdEntity<String>, ISysResourceFormBase

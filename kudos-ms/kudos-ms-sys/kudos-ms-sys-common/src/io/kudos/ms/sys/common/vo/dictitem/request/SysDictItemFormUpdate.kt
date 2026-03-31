package io.kudos.ms.sys.common.vo.dictitem.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 字典项表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val itemCode: String = "",

    override val itemName: String = "",

    override val dictId: String = "",

    override val orderNum: Int? = null,

    override val parentId: String? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, ISysDictItemFormBase

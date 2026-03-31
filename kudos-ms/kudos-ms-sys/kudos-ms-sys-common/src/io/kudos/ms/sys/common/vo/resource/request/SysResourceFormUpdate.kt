package io.kudos.ms.sys.common.vo.resource.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 资源表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val name: String = "",

    override val url: String? = null,

    override val resourceTypeDictCode: String = "",

    override val parentId: String? = null,

    override val orderNum: Int? = null,

    override val icon: String? = null,

    override val subSystemCode: String = "",

    override val remark: String? = null,

) : IIdEntity<String?>, ISysResourceFormBase

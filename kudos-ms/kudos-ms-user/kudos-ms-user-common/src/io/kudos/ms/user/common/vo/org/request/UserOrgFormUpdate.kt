package io.kudos.ms.user.common.vo.org.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 机构表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val name: String? = null,

    override val shortName: String? = null,

    override val tenantId: String? = null,

    override val parentId: String? = null,

    override val orgTypeDictCode: String? = null,

    override val sortNum: Int? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, IUserOrgFormBase

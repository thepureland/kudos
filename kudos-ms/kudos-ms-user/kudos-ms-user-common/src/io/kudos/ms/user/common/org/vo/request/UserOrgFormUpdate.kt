package io.kudos.ms.user.common.org.vo.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Organization form update request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgFormUpdate (

    /** Primary key */
    override val id: String,

    override val name: String?,

    override val shortName: String?,

    override val tenantId: String?,

    override val parentId: String?,

    override val orgTypeDictCode: String?,

    override val sortNum: Int?,

    override val remark: String?,

) : IIdEntity<String>, IUserOrgFormBase

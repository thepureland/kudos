package io.kudos.ms.user.common.org.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Organization edit response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgEdit (

    /** Primary key */
    override val id: String = "",

    /** Organization name */
    val name: String? = null,

    /** Organization short name */
    val shortName: String? = null,

    /** Tenant id */
    val tenantId: String? = null,

    /** Parent organization id */
    val parentId: String? = null,

    /** Organization type dict code */
    val orgTypeDictCode: String? = null,

    /** Sort number */
    val sortNum: Int? = null,

    /** Remark */
    val remark: String? = null,

) : IIdEntity<String>

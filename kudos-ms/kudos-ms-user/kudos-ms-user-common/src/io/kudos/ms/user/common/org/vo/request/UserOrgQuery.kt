package io.kudos.ms.user.common.org.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.org.vo.response.UserOrgRow


/**
 * Organization list query request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgQuery (

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

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserOrgRow::class

}

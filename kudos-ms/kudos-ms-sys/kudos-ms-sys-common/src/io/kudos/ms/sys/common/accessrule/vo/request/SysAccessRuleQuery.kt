package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleRow


/**
 * Access rule list query criteria request VO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleQuery (

    /** Tenant id */
    val tenantId: String? = null,

    /** System code */
    val systemCode: String? = null,

    /** Rule type dict code */
    val accessRuleTypeDictCode: String? = null,

    /** Whether active; when null no filter by active status */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysAccessRuleRow::class

    override fun isUnpagedSearchAllowed(): Boolean = true

}
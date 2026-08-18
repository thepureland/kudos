package io.kudos.ms.sys.common.accessrule.vo.response

import io.kudos.base.model.contract.entity.IIdEntity

/**
 * Access rule list row DTO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleRow (

    /** Primary key. */
    override val id: String = "",

    /** Tenant id. */
    val tenantId: String? = null,

    /** System code. */
    val systemCode: String? = null,

    /** Rule type dictionary code. */
    val accessRuleTypeDictCode: String? = null,

) : IIdEntity<String>
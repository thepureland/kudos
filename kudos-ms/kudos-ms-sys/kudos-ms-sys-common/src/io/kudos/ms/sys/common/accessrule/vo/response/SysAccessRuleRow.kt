package io.kudos.ms.sys.common.accessrule.vo.response

/**
 * Access rule list row DTO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleRow (

    /** Primary key. */
    val id: String = "",

    /** Tenant id. */
    val tenantId: String? = null,

    /** System code. */
    val systemCode: String? = null,

    /** Rule type dictionary code. */
    val accessRuleTypeDictCode: String? = null,

)
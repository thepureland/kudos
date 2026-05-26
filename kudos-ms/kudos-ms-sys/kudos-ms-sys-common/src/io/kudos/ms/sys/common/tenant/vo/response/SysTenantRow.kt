package io.kudos.ms.sys.common.tenant.vo.response

import java.time.LocalDateTime


/**
 * Tenant list query result response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantRow (

    /** Primary key. */
    val id: String = "",

    /** Name. */
    val name: String = "",

    /** Timezone. */
    val timezone: String? = null,

    /** Default language code. */
    val defaultLanguageCode: String? = null,

    /** Create time. */
    val createTime: LocalDateTime? = null,

    /** Remark. */
    val remark: String? = null,

    /** Whether active. */
    val active: Boolean = true,

    /** Whether built-in. */
    val builtIn: Boolean = false,

) {


    /** Comma-separated sub-system codes. */
    var subSystemCodes: String = ""


}
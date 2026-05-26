package io.kudos.ms.sys.common.tenant.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Tenant cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantCacheEntry (

    /** Primary key. */
    override val id: String,


    /** Name. */
    val name: String,

    /** Timezone. */
    val timezone: String?,

    /** Default language code. */
    val defaultLanguageCode: String?,

    /** Remark. */
    val remark: String?,

    /** Whether active. */
    val active: Boolean,

    /** Whether built-in. */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 2728865406469746023L
    }

}

package io.kudos.ms.sys.common.outline.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Cache entry for outbound whitelist.
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineCacheEntry(

    /** Primary key */
    override val id: String,

    /** Name */
    val name: String,

    /** Hostname or wildcard */
    val host: String,

    /** Port; `null` means any port */
    val port: Int?,

    /** Protocol (http/https/tcp/any) */
    val protocol: String,

    /** System code */
    val systemCode: String,

    /** Tenant id; `null` means platform-level */
    val tenantId: String?,

    /** Remark */
    val remark: String?,

    /** Whether enabled */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 5184729285406513961L
    }

}

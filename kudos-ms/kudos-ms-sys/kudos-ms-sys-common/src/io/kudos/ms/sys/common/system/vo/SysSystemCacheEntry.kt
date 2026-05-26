package io.kudos.ms.sys.common.system.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * System cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemCacheEntry (

    override val id: String,

    /** Code */
    val code: String,

    /** Name */
    val name: String,

    /** Whether sub-system */
    val subSystem: Boolean,

    /** Parent system code */
    val parentCode: String?,

    /** Remark */
    val remark: String?,

    /** Whether active */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8383289873002046675L
    }

}

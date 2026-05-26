package io.kudos.ms.sys.common.locale.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Cache entry for language/locale dictionary.
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleCacheEntry(

    /** Primary key */
    override val id: String,

    /** Language code (e.g. zh_CN) */
    val code: String,

    /** Display name */
    val displayName: String,

    /** English name */
    val englishName: String,

    /** Sort number */
    val sortNo: Int,

    /** Remark */
    val remark: String?,

    /** Whether enabled */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 5184729285406513962L
    }

}

package io.kudos.ms.sys.common.dict.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Dictionary cache entry
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictCacheEntry (

    /** Primary key */
    override val id: String,

    /** Dictionary type */
    val dictType: String,

    /** Dictionary name */
    val dictName: String,

    /** Atomic service code */
    val atomicServiceCode: String,

    /** Remark */
    val remark: String?,

    /** Whether enabled */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 7553349815212490728L
    }

}

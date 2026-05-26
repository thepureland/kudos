package io.kudos.ms.sys.common.dict.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Dictionary item cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemCacheEntry (

    /** Primary key */
    override val id: String,

    /** Dictionary item code */
    val itemCode: String,

    /** Dictionary item name */
    val itemName: String,

    /** Dictionary id */
    val dictId: String,

    /** Dictionary item order number */
    val orderNum: Int?,

    /** Parent id */
    val parentId: String?,

    /** Remark */
    val remark: String?,

    /** Whether active */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

    /** Dictionary type (from the v_sys_dict_item view; used as a secondary index for the Hash cache) */
    val dictType: String,

    /** Dictionary name or its i18n key */
    val dictName: String,

    /** Atomic service code (from the v_sys_dict_item view; used as a secondary index for the Hash cache) */
    val atomicServiceCode: String,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 3064983536187872915L
    }

}

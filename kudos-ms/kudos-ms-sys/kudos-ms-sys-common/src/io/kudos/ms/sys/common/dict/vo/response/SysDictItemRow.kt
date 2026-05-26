package io.kudos.ms.sys.common.dict.vo.response

/**
 * Response VO for dict item list query results.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemRow (

    /** Dict item id */
    val id: String = "",

    /** Atomic service code */
    val atomicServiceCode: String = "",

    /** Dict id */
    val dictId: String = "",

    /** Dict type */
    val dictType: String = "",

    /** Dict name */
    val dictName: String = "",

    /** Dict item code */
    val itemCode: String = "",

    /** Parent item id */
    val parentId: String? = null,

    /** Dict item name, or its i18n key */
    val itemName: String = "",

    /** Order number of this item under the same parent */
    val orderNum: Int? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Remark */
    val remark: String? = null,

    /** Parent item code */
    var parentCode: String? = null,

    /** All parent item ids */
    var parentIds: List<String>? = null

)
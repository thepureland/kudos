package io.kudos.ms.sys.common.dict.vo.response

/**
 * Response VO for dict list query results.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictRow (

    /** Primary key */
    val id: String = "",

    /** Dict type */
    val dictType: String = "",

    /** Dict name */
    val dictName: String = "",

    /** Atomic service code */
    val atomicServiceCode: String = "",

    val parentId: String? = null,

    val parentCode: String? = null,

    val parentIds: List<String>? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = true,

    val itemId: String? = null, //TODO

    val itemCode: String? = null, //TODO

    val itemName: String? = null, //TODO

    val seqNo: Int? = null, //TODO

)
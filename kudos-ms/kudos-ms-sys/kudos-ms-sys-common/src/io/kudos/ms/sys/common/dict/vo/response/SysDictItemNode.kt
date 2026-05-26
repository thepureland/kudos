package io.kudos.ms.sys.common.dict.vo.response

/**
 * Response VO for a dict item tree node.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemNode(

    /** Primary key */
    val id: String,

    /** Dict item code */
    val itemCode: String = "",

    /** Dict item name */
    val itemName: String = "",

)
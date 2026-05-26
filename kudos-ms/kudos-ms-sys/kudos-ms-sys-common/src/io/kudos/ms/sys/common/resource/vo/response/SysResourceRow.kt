package io.kudos.ms.sys.common.resource.vo.response

/**
 * Resource list query result response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceRow (

    /** Primary key */
    val id: String = "",

    /** Name */
    val name: String = "",

    /** url */
    val url: String? = null,

    /** Resource type dict code */
    val resourceTypeDictCode: String = "",

    /** Parent id */
    val parentId: String? = null,

    /** Order number among siblings under the same parent */
    val orderNum: Int? = null,

    /** Icon */
    val icon: String? = null,

    /** Sub-system code */
    val subSystemCode: String = "",

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = false,

)
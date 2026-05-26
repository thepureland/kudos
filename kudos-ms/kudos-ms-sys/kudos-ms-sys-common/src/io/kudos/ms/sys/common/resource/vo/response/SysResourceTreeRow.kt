package io.kudos.ms.sys.common.resource.vo.response

/**
 * Resource tree list query result response VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceTreeRow (

    /** Primary key */
    val id: String = "",

    /** Name */
    val name: String? = null,

    /** URL */
    val url: String? = null,

    /** Resource type dictionary code */
    val resourceTypeDictCode: String? = null,

    /** Parent id */
    val parentId: String? = null,

    /** Order number among siblings under the same parent */
    val orderNum: Int? = null,

    /** Icon */
    val icon: String? = null,

    /** Subsystem code */
    val subSystemCode: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

    /** Child resource list */
    val children: MutableList<SysResourceTreeRow>? = null,

)
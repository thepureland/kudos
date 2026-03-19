package io.kudos.ms.sys.common.vo.resource.response


/**
 * 资源树列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceTreeRow (

    /** 主键 */
    val id: String = "",

    /** 名称 */
    val name: String? = null,

    /** url */
    val url: String? = null,

    /** 资源类型字典代码 */
    val resourceTypeDictCode: String? = null,

    /** 父id */
    val parentId: String? = null,

    /** 在同父节点下的排序号 */
    val orderNum: Int? = null,

    /** 图标 */
    val icon: String? = null,

    /** 子系统编码 */
    val subSystemCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 子资源列表 */
    val children: MutableList<SysResourceTreeRow>? = null,

)
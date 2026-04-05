package io.kudos.ms.sys.common.system.vo.response
/**
 * 系统列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemRow (

    /** 主键 */
    val id: String = "",

    /** 编码 */
    val code: String = "",

    /** 名称 */
    val name: String = "",

    /** 是否子系统 */
    val subSystem: Boolean = true,

    /** 父系统编号 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

)
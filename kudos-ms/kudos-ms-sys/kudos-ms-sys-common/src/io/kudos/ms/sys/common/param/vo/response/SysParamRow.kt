package io.kudos.ms.sys.common.param.vo.response

/**
 * 参数列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamRow (

    /** 主键 */
    val id: String = "",

    /** 参数名称 */
    val paramName: String = "",

    /** 参数值 */
    val paramValue: String = "",

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 原子服务编码  */
    val atomicServiceCode: String = "",

    /** 序号 */
    val orderNum: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

)
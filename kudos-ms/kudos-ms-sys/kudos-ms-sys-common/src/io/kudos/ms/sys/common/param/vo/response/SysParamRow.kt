package io.kudos.ms.sys.common.param.vo.response

/**
 * Parameter list query result response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamRow (

    /** Primary key */
    val id: String = "",

    /** Parameter name */
    val paramName: String = "",

    /** Parameter value */
    val paramValue: String = "",

    /** Default parameter value */
    val defaultValue: String? = null,

    /** Atomic service code */
    val atomicServiceCode: String = "",

    /** Order number */
    val orderNum: Int? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = true,

)
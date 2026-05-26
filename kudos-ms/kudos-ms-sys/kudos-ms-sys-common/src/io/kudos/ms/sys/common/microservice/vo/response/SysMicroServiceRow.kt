package io.kudos.ms.sys.common.microservice.vo.response

/**
 * Response VO for microservice list query result.
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceRow (

    /** Primary key */
    val id: String = "",

    /** Code */
    val code: String = "",

    /** Name */
    val name: String = "",

    /** Context */
    val context: String = "",

    /** Whether atomic service */
    val atomicService: Boolean = true,

    /** Parent service code */
    val parentCode: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = true,

)
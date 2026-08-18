package io.kudos.ms.sys.common.microservice.vo.response

import io.kudos.base.model.contract.entity.IIdEntity

/**
 * Response VO for microservice list query result.
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceRow (

    /** Primary key */
    override val id: String = "",

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

) : IIdEntity<String>
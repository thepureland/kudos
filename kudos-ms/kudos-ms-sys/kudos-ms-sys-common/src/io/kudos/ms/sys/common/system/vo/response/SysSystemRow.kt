package io.kudos.ms.sys.common.system.vo.response

import io.kudos.base.model.contract.entity.IIdEntity

/**
 * System list query result response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemRow (

    /** Primary key */
    override val id: String = "",

    /** Code */
    val code: String = "",

    /** Name */
    val name: String = "",

    /** Whether it is a subsystem */
    val subSystem: Boolean = true,

    /** Parent system code */
    val parentCode: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether active */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = false,

) : IIdEntity<String>
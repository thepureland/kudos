package io.kudos.ms.sys.common.system.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * System detail response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemDetail (

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

    /** Creator id */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
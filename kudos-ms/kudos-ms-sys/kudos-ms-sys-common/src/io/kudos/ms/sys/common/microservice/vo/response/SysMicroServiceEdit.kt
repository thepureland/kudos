package io.kudos.ms.sys.common.microservice.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for microservice edit.
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceEdit (

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

    /** Creator id */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Creation time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
package io.kudos.ms.user.common.contact.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * User contact way list query result response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayRow (

    /** Primary key */
    override val id: String = "",

    /** User ID */
    val userId: String? = null,

    /** Contact way dict code */
    val contactWayDictCode: String? = null,

    /** Contact way value */
    val contactWayValue: String? = null,

    /** Contact way status dict code */
    val contactWayStatusDictCode: String? = null,

    /** Priority */
    val priority: Short? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

    /** Creator ID */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater ID */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
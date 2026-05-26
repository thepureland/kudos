package io.kudos.ms.sys.common.dict.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Dictionary edit response VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictEdit (

    /** Primary key */
    override val id: String = "",


    /** Dictionary type */
    val dictType: String = "",

    /** Dictionary name */
    val dictName: String = "",

    /** Atomic service code */
    val atomicServiceCode: String = "",

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

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
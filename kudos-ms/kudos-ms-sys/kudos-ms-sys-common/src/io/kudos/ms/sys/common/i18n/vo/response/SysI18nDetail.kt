package io.kudos.ms.sys.common.i18n.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for i18n details.
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nDetail (

    /** Primary key */
    override val id: String = "",


    /** Language_Region */
    val locale: String = "",

    /** Atomic service code */
    val atomicServiceCode: String = "",

    /** I18n type dictionary code */
    val i18nTypeDictCode: String = "",

    /** I18n namespace */
    val namespace: String = "",

    /** I18n key */
    val key: String = "",

    /** I18n value */
    val value: String = "",

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
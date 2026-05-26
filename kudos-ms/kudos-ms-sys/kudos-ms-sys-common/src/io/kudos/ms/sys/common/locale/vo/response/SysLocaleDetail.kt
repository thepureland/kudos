package io.kudos.ms.sys.common.locale.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for language dictionary details.
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleDetail(

    /** Primary key */
    override val id: String = "",

    /** Language code */
    val code: String = "",

    /** Display name */
    val displayName: String = "",

    /** English name */
    val englishName: String = "",

    /** Sort number */
    val sortNo: Int = 0,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = false,

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

package io.kudos.ms.sys.common.accessrule.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Access rule edit-page rendering DTO (with audit fields).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleEdit (

    override val id: String = "",

    /** Tenant id. */
    val tenantId: String? = null,

    /** System code. */
    val systemCode: String? = null,

    /** Rule type dictionary code. */
    val accessRuleTypeDictCode: String? = null,

    /** Remark. */
    val remark: String? = null,

    /** Creator id. */
    val createUserId: String? = null,

    /** Creator name. */
    val createUserName: String? = null,

    /** Create time. */
    val createTime: LocalDateTime? = null,

    /** Updater id. */
    val updateUserId: String? = null,

    /** Updater name. */
    val updateUserName: String? = null,

    /** Update time. */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>
package io.kudos.ms.sys.common.outline.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Response VO for outbound whitelist details.
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineDetail(

    /** Primary key */
    override val id: String = "",

    /** Name */
    val name: String = "",

    /** Hostname or wildcard */
    val host: String = "",

    /** Port */
    val port: Int? = null,

    /** Protocol */
    val protocol: String = "",

    /** System code */
    val systemCode: String = "",

    /** Tenant id */
    val tenantId: String? = null,

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

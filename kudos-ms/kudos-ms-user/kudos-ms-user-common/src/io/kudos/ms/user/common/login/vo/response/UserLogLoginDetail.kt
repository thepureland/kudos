package io.kudos.ms.user.common.login.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Login log detail response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLogLoginDetail (

    /** Primary key */
    override val id: String = "",

    /** User ID */
    val userId: String? = null,

    /** Username */
    val username: String? = null,

    /** Tenant ID */
    val tenantId: String? = null,

    /** Login time */
    val loginTime: LocalDateTime? = null,

    /** Login IP */
    val loginIp: Long? = null,

    /** Login location */
    val loginLocation: String? = null,

    /** Login device */
    val loginDevice: String? = null,

    /** Browser */
    val loginBrowser: String? = null,

    /** Operating system */
    val loginOs: String? = null,

    /** User agent string */
    val userAgent: String? = null,

    /** Whether login succeeded */
    val loginSuccess: Boolean? = null,

    /** Failure reason */
    val failureReason: String? = null,

    /** Session ID */
    val sessionId: String? = null,

    /** Remark */
    val remark: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

) : IIdEntity<String>
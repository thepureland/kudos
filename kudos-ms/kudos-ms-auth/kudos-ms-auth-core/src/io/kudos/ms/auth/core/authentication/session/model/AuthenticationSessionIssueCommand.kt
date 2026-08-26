package io.kudos.ms.auth.core.authentication.session.model

import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext

/** Trusted, server-built facts used to register a newly issued local session. */
data class AuthenticationSessionIssueCommand(
    val context: AuthenticationContext,
    val username: String? = null,
    val clientId: String? = null,
    val deviceId: String? = null,
    val loginIp: Long? = null,
    val loginDevice: String? = null,
    val loginBrowser: String? = null,
    val loginOs: String? = null,
    val userAgent: String? = null,
    /** Optional trusted override for persistent/API sessions; null uses the browser-session default. */
    val idleTimeoutSeconds: Long? = null,
    /** Optional trusted override for persistent/API sessions; null uses the browser-session default. */
    val absoluteTimeoutSeconds: Long? = null,
)

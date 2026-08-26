package io.kudos.ms.user.core.account.model

import java.time.LocalDateTime

/** Append-only audit input for one binding lifecycle decision. */
data class UserAccountThirdAuditEvent(
    val bindingId: String? = null,
    val userId: String,
    val tenantId: String,
    val identityProviderId: String?,
    val providerCode: String,
    val subject: String,
    val action: String,
    val success: Boolean,
    val reason: String? = null,
    val actorUserId: String,
    val operationReason: String? = null,
    val beforeSnapshot: UserAccountThirdAuditSnapshot? = null,
    val afterSnapshot: UserAccountThirdAuditSnapshot? = null,
    val eventTime: LocalDateTime = LocalDateTime.now(),
)

/** Minimal immutable snapshot; the audit serializer hashes [subject] before persistence. */
data class UserAccountThirdAuditSnapshot(
    val identityProviderId: String?,
    val providerCode: String,
    val issuer: String?,
    val subject: String,
    val active: Boolean,
)

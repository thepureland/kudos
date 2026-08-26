package io.kudos.ms.user.core.account.service.impl

import io.kudos.ms.user.core.account.dao.UserAccountThirdAuditDao
import io.kudos.ms.user.core.account.model.UserAccountThirdAuditEvent
import io.kudos.ms.user.core.account.model.UserAccountThirdAuditSnapshot
import io.kudos.ms.user.core.account.model.po.UserAccountThirdAudit
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdAuditService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

/** Persists binding audit independently so a denied operation cannot roll its audit row back. */
@Service
open class UserAccountThirdAuditService(
    private val dao: UserAccountThirdAuditDao,
) : IUserAccountThirdAuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun record(event: UserAccountThirdAuditEvent): String = dao.insert(
        UserAccountThirdAudit {
            bindingId = event.bindingId
            userId = event.userId.take(36)
            tenantId = event.tenantId.take(36)
            identityProviderId = event.identityProviderId?.take(36)
            providerCode = event.providerCode.take(32)
            subjectHash = sha256(event.subject)
            action = event.action.take(16)
            success = event.success
            reason = event.reason?.take(128)
            actorUserId = event.actorUserId.take(36)
            operationReason = event.operationReason?.take(512)
            beforeSnapshot = event.beforeSnapshot?.let(::serializeSnapshot)
            afterSnapshot = event.afterSnapshot?.let(::serializeSnapshot)
            eventTime = event.eventTime
        }
    )

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun serializeSnapshot(snapshot: UserAccountThirdAuditSnapshot): String = listOf(
        "identityProviderId=${snapshot.identityProviderId.orEmpty()}",
        "providerCode=${snapshot.providerCode}",
        "issuer=${snapshot.issuer.orEmpty()}",
        "subjectHash=${sha256(snapshot.subject)}",
        "active=${snapshot.active}",
    ).joinToString(";").take(2048)
}

package io.kudos.ms.auth.core.platform.authz.audit

import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.platform.authz.init.properties.AuthzProperties
import org.springframework.beans.factory.support.StaticListableBeanFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * @author K
 * @author AI: Codex
 */
internal class AuthzDecisionAuditRecorderTest {

    @Test
    fun deniedDecisionIsRecordedWithoutContextValues() {
        var captured: AuthzDecisionAuditRecord? = null
        val recorder = recorder(AuthzProperties(), IAuthzDecisionAuditSink { captured = it })
        recorder.record(
            AuthzRequest(SubjectRef.ofUser("u1"), "orders:delete", mapOf("token" to "secret")),
            AuthzDecision.deny(AuthzDecision.Reason.DENIED_BY_DEFAULT),
        )

        assertEquals(setOf("token"), captured?.contextKeys)
        assertNull(captured?.matchedRoleId)
    }

    @Test
    fun auditFailureCanBeConfiguredFailClosed() {
        val properties = AuthzProperties().apply { failOnAuditError = true }
        val recorder = recorder(properties, IAuthzDecisionAuditSink { error("sink unavailable") })
        assertFailsWith<IllegalStateException> {
            recorder.record(
                AuthzRequest(SubjectRef.ofUser("u1"), "payments:approve"),
                AuthzDecision.deny(AuthzDecision.Reason.DENIED_BY_DEFAULT),
            )
        }
    }

    private fun recorder(properties: AuthzProperties, sink: IAuthzDecisionAuditSink): AuthzDecisionAuditRecorder {
        val factory = StaticListableBeanFactory(mapOf("sink" to sink))
        return AuthzDecisionAuditRecorder(factory.getBeanProvider(IAuthzDecisionAuditSink::class.java), properties)
    }
}

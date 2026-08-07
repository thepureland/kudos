package io.kudos.ms.auth.client.authz.fallback

import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author K
 * @author AI: Codex
 */
internal class AuthzDecisionFallbackTest {

    @Test
    fun remoteFailureAlwaysDeniesWithAnExplicitReason() {
        val decision = AuthzDecisionFallback().decide(
            AuthzRequest(SubjectRef.ofUser("u1"), "payments:approve"),
        )
        assertFalse(decision.permitted)
        assertTrue(decision.detail.orEmpty().contains("fail-closed"))
        assertTrue(decision.reason == AuthzDecision.Reason.DENIED_BY_DEFAULT)
    }
}

package io.kudos.ms.auth.api.internal.controller.authz

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * @author K
 * @author AI: Codex
 */
internal class AuthzDecisionInternalControllerTest {

    @Test
    fun delegatesTheRequestAndReturnsTheDecisionUnchanged() {
        val api = mock(IAuthzDecisionApi::class.java)
        val controller = AuthzDecisionInternalController(api)
        val request = AuthzRequest(SubjectRef.ofUser("u1"), "orders:read")
        val decision = AuthzDecision.permit(AuthzDecision.Reason.ALLOWED_BY_GRANT, "orders:read")
        whenCalled(api.decide(request)).thenReturn(decision)

        assertSame(decision, controller.decide(request))
        verify(api).decide(request)
    }
}

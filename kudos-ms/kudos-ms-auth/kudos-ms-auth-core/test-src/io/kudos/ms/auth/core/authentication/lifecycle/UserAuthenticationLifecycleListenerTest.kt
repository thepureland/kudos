package io.kudos.ms.auth.core.authentication.lifecycle

import io.kudos.ms.auth.core.authentication.lifecycle.listener.UserAuthenticationLifecycleListener
import io.kudos.ms.auth.core.authentication.lifecycle.service.iservice.IAuthenticationLifecycleService
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAuthenticationInvalidated
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test

internal class UserAuthenticationLifecycleListenerTest {

    private val lifecycleService = mock(IAuthenticationLifecycleService::class.java)
    private val listener = UserAuthenticationLifecycleListener(lifecycleService)

    @Test
    fun securityChangeUsesItsPreciseReason() {
        listener.on(
            UserAuthenticationInvalidated(
                "u-1",
                "t-1",
                UserAuthenticationInvalidated.Reason.LOGIN_PASSWORD_CHANGED,
            )
        )

        verify(lifecycleService).invalidateAll("t-1", "u-1", "LOGIN_PASSWORD_CHANGED")
    }

    @Test
    fun webAuthnCredentialChangeInvalidatesExistingSessionsAndTokens() {
        listener.on(
            UserAuthenticationInvalidated(
                "u-1",
                "t-1",
                UserAuthenticationInvalidated.Reason.WEBAUTHN_CREDENTIAL_CHANGED,
            )
        )

        verify(lifecycleService).invalidateAll("t-1", "u-1", "WEBAUTHN_CREDENTIAL_CHANGED")
    }

    @Test
    fun deletedAccountsRetainTenantScopeForSingleAndBatchEvents() {
        listener.on(UserAccountDeleted("u-1", "t-1", "alice"))
        listener.on(
            UserAccountBatchDeleted(
                listOf(
                    UserAccountBatchDeleted.Item("u-2", "t-2", "bob"),
                    UserAccountBatchDeleted.Item("u-3", "t-3", "carol"),
                )
            )
        )

        verify(lifecycleService).invalidateAll("t-1", "u-1", "ACCOUNT_DELETED")
        verify(lifecycleService).invalidateAll("t-2", "u-2", "ACCOUNT_DELETED")
        verify(lifecycleService).invalidateAll("t-3", "u-3", "ACCOUNT_DELETED")
    }
}

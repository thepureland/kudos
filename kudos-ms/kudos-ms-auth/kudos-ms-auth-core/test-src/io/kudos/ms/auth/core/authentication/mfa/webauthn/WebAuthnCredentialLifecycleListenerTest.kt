package io.kudos.ms.auth.core.authentication.mfa.webauthn

import io.kudos.ms.auth.core.authentication.mfa.webauthn.dao.AuthWebAuthnCredentialDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.listener.WebAuthnCredentialLifecycleListener
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test

internal class WebAuthnCredentialLifecycleListenerTest {
    private val dao = mock(AuthWebAuthnCredentialDao::class.java)
    private val listener = WebAuthnCredentialLifecycleListener(dao)

    @Test
    fun accountDeletionRemovesSingleAndBatchCredentialRows() {
        listener.on(UserAccountDeleted("u-1", "t-1", "alice"))
        listener.on(
            UserAccountBatchDeleted(
                listOf(
                    UserAccountBatchDeleted.Item("u-2", "t-1", "bob"),
                    UserAccountBatchDeleted.Item("u-3", "t-2", "carol"),
                )
            )
        )

        verify(dao).deleteByUserId("u-1")
        verify(dao).deleteByUserId("u-2")
        verify(dao).deleteByUserId("u-3")
    }
}

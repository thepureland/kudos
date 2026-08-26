package io.kudos.ms.auth.core.authentication.mfa.recovery

import io.kudos.ms.auth.core.authentication.mfa.recovery.dao.AuthRecoveryCodeDao
import io.kudos.ms.auth.core.authentication.mfa.recovery.listener.RecoveryCodeLifecycleListener
import io.kudos.ms.user.core.account.event.UserAuthenticationInvalidated
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalDateTime
import kotlin.test.Test

internal class RecoveryCodeLifecycleListenerTest {

    private val dao = mock(AuthRecoveryCodeDao::class.java)
    private val listener = RecoveryCodeLifecycleListener(dao)

    @Test
    fun authenticatorChangeRevokesEveryLiveCodeSet() {
        listener.on(
            UserAuthenticationInvalidated(
                id = "u-1",
                tenantId = "t-1",
                reason = UserAuthenticationInvalidated.Reason.AUTHENTICATOR_CHANGED,
            )
        )

        verify(dao).revokeActive(
            eqString("t-1"),
            eqString("u-1"),
            anyDateTime(),
        )
    }

    @Test
    fun passwordChangeDoesNotRevokeRecoveryCodes() {
        listener.on(
            UserAuthenticationInvalidated(
                id = "u-1",
                tenantId = "t-1",
                reason = UserAuthenticationInvalidated.Reason.LOGIN_PASSWORD_CHANGED,
            )
        )

        verify(dao, never()).revokeActive(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            anyDateTime(),
        )
    }

    @Test
    fun webAuthnCredentialChangeDoesNotDestroyTotpRecoveryCodes() {
        listener.on(
            UserAuthenticationInvalidated(
                id = "u-1",
                tenantId = "t-1",
                reason = UserAuthenticationInvalidated.Reason.WEBAUTHN_CREDENTIAL_CHANGED,
            )
        )

        verify(dao, never()).revokeActive(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            anyDateTime(),
        )
    }

    private fun anyDateTime(): LocalDateTime =
        ArgumentMatchers.any(LocalDateTime::class.java) ?: LocalDateTime.MIN

    private fun eqString(value: String): String = ArgumentMatchers.eq(value) ?: value
}

package io.kudos.ms.auth.core.authentication.credential

import io.kudos.base.security.PasswordKit
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialException
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialSecretTypeEnum
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialStoreCommand
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialSummary
import io.kudos.ms.auth.core.authentication.credential.service.iservice.IAuthCredentialService
import io.kudos.ms.user.core.account.security.IAccountCredentialStore
import io.kudos.ms.user.core.account.security.IPasswordHistory
import io.kudos.ms.user.core.account.security.PasswordPolicyContext
import io.kudos.ms.user.core.account.security.PasswordPurpose
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Real Flyway/H2 regression for the V61 cutover: the login password verifies, rotates and re-encodes through
 * `auth_credential` rather than `user_account.login_password`.
 */
@EnabledIfDockerInstalled
internal class AuthAccountCredentialStoreTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var credentialStore: IAccountCredentialStore

    @Resource
    private lateinit var credentialService: IAuthCredentialService

    @Resource
    private lateinit var passwordEncoder: PasswordEncoder

    @Resource
    private lateinit var passwordHistories: List<IPasswordHistory>

    @Test
    fun v61EnrolsThenVerifiesTheLoginPasswordThroughTheCredentialStore() {
        val context = context()

        assertFalse(credentialStore.hasPassword(context))
        assertFalse(credentialStore.verifyPassword("Correct-Horse-1", context))

        credentialStore.storePassword(encode("Correct-Horse-1"), context)

        assertTrue(credentialStore.hasPassword(context))
        assertTrue(credentialStore.verifyPassword("Correct-Horse-1", context))
        assertFalse(credentialStore.verifyPassword("Correct-Horse-2", context))
    }

    @Test
    fun v61RotatesAndFilesTheOutgoingPasswordInHistory() {
        val context = context()
        credentialStore.storePassword(encode("First-Password-1"), context)

        credentialStore.storePassword(encode("Second-Password-2"), context)

        assertTrue(credentialStore.verifyPassword("Second-Password-2", context))
        assertFalse(credentialStore.verifyPassword("First-Password-1", context))
        // The retiring hash is filed by the store itself: user-core reads it from a column that is now empty,
        // so without this the reuse check would quietly stop rejecting anything.
        assertTrue(
            passwordHistories.isNotEmpty() && passwordHistories.any { it.isReused("First-Password-1", context) },
            "The retired password should have been recorded in history",
        )
    }

    @Test
    fun v61ReEncodesAnOutdatedEncodingWithoutChangingThePassword() {
        val context = context()
        // A bare legacy BCrypt hash: PasswordEncodingKit treats an unprefixed hash as always due an upgrade.
        credentialService.store(
            command(context, BCrypt.hashpw("Legacy-Password-1", BCrypt.gensalt(4)))
        )
        val before = activeCredential(context)

        assertTrue(credentialStore.verifyPassword("Legacy-Password-1", context))
        assertTrue(credentialStore.upgradePasswordEncodingIfNeeded("Legacy-Password-1", context))

        assertEquals(before.version + 1, activeCredential(context).version)
        // Same password, stronger encoding — the point of the upgrade is that logins keep working.
        assertTrue(credentialStore.verifyPassword("Legacy-Password-1", context))
        // Already current now, so a second call is a no-op rather than another rotation.
        assertFalse(credentialStore.upgradePasswordEncodingIfNeeded("Legacy-Password-1", context))
    }

    @Test
    fun v61RefusesToOverwriteAPasswordChangeThatWonTheRace() {
        val context = context()
        credentialStore.storePassword(encode("Original-Password-1"), context)
        val stale = activeCredential(context)
        credentialStore.storePassword(encode("Winner-Password-2"), context)

        // A writer still holding the pre-change version must be refused rather than reinstating its password.
        assertEquals(
            "AUTH_CREDENTIAL_VERSION_CONFLICT",
            assertFailsWith<AuthCredentialException> {
                credentialService.store(
                    command(context, encode("Loser-Password-3"), expectedVersion = stale.version)
                )
            }.errorCode,
        )
        assertTrue(credentialStore.verifyPassword("Winner-Password-2", context))
    }

    @Test
    fun v61TreatsAMissingTenantOrUserAsAWiringFaultRatherThanAWrongPassword() {
        // Answering "false" here would read to a caller as a wrong password and hide the misconfiguration.
        assertFailsWith<IllegalStateException> {
            credentialStore.verifyPassword(
                "anything",
                PasswordPolicyContext(PasswordPurpose.LOGIN, userId = "u", tenantId = " "),
            )
        }
        assertFailsWith<IllegalStateException> {
            credentialStore.hasPassword(PasswordPolicyContext(PasswordPurpose.LOGIN, userId = null, tenantId = "t"))
        }
    }

    @Test
    fun v61KeepsAPasswordSeededAsALegacyHashVerifiableAcrossTheUpgrade() {
        val context = context()
        // The shape a legacy row has: a bare BCrypt hash produced by PasswordKit, exactly as the recovery-code
        // login regression seeds it. That path re-encodes on the first successful sign-in.
        credentialStore.storePassword(PasswordKit.hash(PASSWORD_TEXT, strength = 4), context)
        assertTrue(credentialStore.verifyPassword(PASSWORD_TEXT, context))

        assertTrue(credentialStore.upgradePasswordEncodingIfNeeded(PASSWORD_TEXT, context))

        assertTrue(
            credentialStore.verifyPassword(PASSWORD_TEXT, context),
            "The password must still verify after its encoding was upgraded",
        )
    }

    private fun activeCredential(context: PasswordPolicyContext): AuthCredentialSummary = assertNotNull(
        credentialService.findActive(context.tenantId!!, context.userId!!, AuthCredentialSecretTypeEnum.PASSWORD)
    )

    private fun command(
        context: PasswordPolicyContext,
        secret: String,
        expectedVersion: Long? = null,
    ) = AuthCredentialStoreCommand(
        tenantId = context.tenantId!!,
        userId = context.userId!!,
        type = AuthCredentialSecretTypeEnum.PASSWORD,
        secretHashOrRef = secret,
        expectedVersion = expectedVersion,
    )

    private fun encode(password: String): String =
        requireNotNull(passwordEncoder.encode(password)) { "Password encoder returned null" }

    private fun context() = PasswordPolicyContext(
        purpose = PasswordPurpose.LOGIN,
        userId = UUID.randomUUID().toString(),
        username = "user-${UUID.randomUUID()}".take(30),
        tenantId = UUID.randomUUID().toString(),
    )

    private companion object {
        /** The same password the recovery-code login regression uses, so the two exercise one shape. */
        const val PASSWORD_TEXT = "recovery-test-password"
    }
}

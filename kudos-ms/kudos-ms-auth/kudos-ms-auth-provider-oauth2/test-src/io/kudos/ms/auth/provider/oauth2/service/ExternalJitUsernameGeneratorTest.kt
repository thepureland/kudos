package io.kudos.ms.auth.provider.oauth2.service

import io.kudos.ms.auth.common.provider.enums.ExternalProtocolEnum
import io.kudos.ms.auth.common.provider.enums.ExternalJitUsernameStrategyEnum
import io.kudos.ms.auth.common.provider.vo.ExternalPrincipal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class ExternalJitUsernameGeneratorTest {

    @Test
    fun usernameIsStableBoundedAndKeepsInternationalLetters() {
        val resolved = resolved(username = "  山田 太郎 / Admin  ")

        val first = ExternalJitUsernameGenerator.generate(resolved)
        val second = ExternalJitUsernameGenerator.generate(resolved)

        assertEquals(first, second)
        assertTrue(first.startsWith("山田_太郎_admin_"))
        assertTrue(first.length <= 32)
    }

    @Test
    fun mutableUsernameDoesNotControlIdentityUniqueness() {
        val first = ExternalJitUsernameGenerator.generate(resolved(username = "alice", subject = "subject-1"))
        val renamed = ExternalJitUsernameGenerator.generate(resolved(username = "renamed", subject = "subject-1"))
        val anotherIdentity = ExternalJitUsernameGenerator.generate(resolved(username = "alice", subject = "subject-2"))

        assertNotEquals(first, renamed)
        assertNotEquals(first, anotherIdentity)
        assertEquals(first.substringAfterLast('_'), renamed.substringAfterLast('_'))
    }

    @Test
    fun emailAndOpaqueStrategiesKeepTheSameStableIdentitySuffix() {
        val resolved = resolved(username = "Alice", subject = "subject-1")
        val withEmail = resolved.copy(principal = resolved.principal.copy(email = "person@example.com"))

        val email = ExternalJitUsernameGenerator.generate(
            withEmail, ExternalJitUsernameStrategyEnum.EMAIL_LOCAL_PART_HASHED,
        )
        val opaque = ExternalJitUsernameGenerator.generate(
            withEmail, ExternalJitUsernameStrategyEnum.OPAQUE_HASHED,
        )

        assertTrue(email.startsWith("person_"))
        assertTrue(opaque.startsWith("ext_"))
        assertEquals(32, opaque.length)
        assertTrue(opaque.removePrefix("ext_").startsWith(email.substringAfterLast('_')))
    }

    private fun resolved(username: String?, subject: String = "subject-1") = ResolvedExternalPrincipal(
        tenantId = "tenant-1",
        providerId = "provider-1",
        providerCode = "google",
        principal = ExternalPrincipal(
            providerId = "provider-1",
            protocol = ExternalProtocolEnum.OIDC,
            issuer = "https://accounts.google.com",
            subject = subject,
            username = username,
        ),
    )
}

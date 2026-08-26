package io.kudos.ms.auth.provider.webauthn.init

import com.yubico.webauthn.attestation.AttestationTrustSource
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ms.auth.provider.webauthn.ceremony.IWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.ceremony.InMemoryWebAuthnCeremonyStore
import io.kudos.ms.auth.provider.webauthn.ceremony.RedisWebAuthnCeremonyStore
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class AuthProviderWebAuthnAutoConfigurationTest {
    private val runner = ApplicationContextRunner()
        .withUserConfiguration(AuthProviderWebAuthnAutoConfiguration::class.java)

    @Test
    fun `should expose component name`() {
        assertEquals(
            "kudos-ms-auth-provider-webauthn",
            AuthProviderWebAuthnAutoConfiguration().getComponentName(),
        )
    }

    @Test
    fun `should use in memory store when redis is unavailable`() {
        runner.run { context ->
            assertIs<InMemoryWebAuthnCeremonyStore>(context.getBean(IWebAuthnCeremonyStore::class.java))
            assertEquals(0, context.getBeansOfType(AttestationTrustSource::class.java).size)
        }
    }

    @Test
    fun `should prefer redis store when redis templates are available`() {
        runner
            .withBean(RedisTemplates::class.java, { mock(RedisTemplates::class.java) })
            .run { context ->
                assertIs<RedisWebAuthnCeremonyStore>(context.getBean(IWebAuthnCeremonyStore::class.java))
            }
    }
}

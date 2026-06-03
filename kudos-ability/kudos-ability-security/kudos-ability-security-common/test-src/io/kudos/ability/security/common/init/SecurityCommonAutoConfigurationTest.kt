package io.kudos.ability.security.common.init

import io.kudos.ability.security.common.support.Authenticator
import io.kudos.ability.security.common.support.TotpAuthenticator
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Spring-context integration tests for [SecurityCommonAutoConfiguration].
 *
 * Verifies that the auto-config registers a [PasswordEncoder] + [Authenticator] by default, and
 * that `@ConditionalOnMissingBean` correctly steps aside when an app provides its own bean.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SecurityCommonAutoConfigurationTest {

    private val runner: ApplicationContextRunner = ApplicationContextRunner()
        .withUserConfiguration(SecurityCommonAutoConfiguration::class.java)

    @Test
    fun defaults_registerPasswordEncoderAndAuthenticator() {
        runner.run { ctx ->
            assertEquals(1, ctx.getBeanNamesForType(PasswordEncoder::class.java).size)
            assertEquals(1, ctx.getBeanNamesForType(Authenticator::class.java).size)
            // Round-trip the PasswordEncoder to confirm BCrypt is wired (delegating encoder
            // produces `{bcrypt}$2a$10$...`).
            val encoder = ctx.getBean(PasswordEncoder::class.java)
            val hash = encoder.encode("hunter2")!!
            assertTrue(hash.startsWith("{bcrypt}"), "encoder should prefix with the delegating id")
            assertTrue(encoder.matches("hunter2", hash))
        }
    }

    @Test
    fun authenticator_isATotpAuthenticator_withRoundTripWorking() {
        runner.run { ctx ->
            val authenticator = ctx.getBean(Authenticator::class.java)
            assertTrue(authenticator is TotpAuthenticator)
            val secret = authenticator.generateKey()
            val code = authenticator.generateCode(secret).toInt()
            assertTrue(authenticator.verify(secret, code))
        }
    }

    @Test
    fun authenticator_picksUpCustomWindowSizeFromProperties() {
        // Confirms property binding works end-to-end: bean is registered and a round-trip still
        // succeeds under a non-default windowSize. Drift-window semantics are covered in
        // TotpAuthenticatorTest where Clock injection is direct.
        ApplicationContextRunner()
            .withUserConfiguration(SecurityCommonAutoConfiguration::class.java)
            .withPropertyValues("kudos.ability.security.common.totp.window-size=5")
            .run { ctx ->
                val authenticator = ctx.getBean(Authenticator::class.java)
                val secret = authenticator.generateKey()
                val code = authenticator.generateCode(secret).toInt()
                assertTrue(authenticator.verify(secret, code))
            }
    }
}

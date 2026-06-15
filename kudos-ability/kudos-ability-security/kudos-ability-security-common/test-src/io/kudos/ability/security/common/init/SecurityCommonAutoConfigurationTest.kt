package io.kudos.ability.security.common.init

import io.kudos.ability.security.common.support.Authenticator
import io.kudos.ability.security.common.support.TotpAuthenticator
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
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

    @Test
    fun conditionalOnMissingBean_backsOff_whenAppProvidesItsOwnBeans() {
        // The KDoc contract: apps can override either bean without excluding the module. With a
        // user-supplied PasswordEncoder and Authenticator, the auto-config must not register its
        // own — the context should expose exactly the custom instances.
        ApplicationContextRunner()
            .withUserConfiguration(
                CustomBeansConfiguration::class.java,
                SecurityCommonAutoConfiguration::class.java,
            )
            .run { ctx ->
                assertEquals(1, ctx.getBeanNamesForType(PasswordEncoder::class.java).size)
                assertEquals(1, ctx.getBeanNamesForType(Authenticator::class.java).size)
                assertSame(CustomBeansConfiguration.ENCODER, ctx.getBean(PasswordEncoder::class.java))
                assertSame(CustomBeansConfiguration.AUTHENTICATOR, ctx.getBean(Authenticator::class.java))
            }
    }

    @Test
    fun componentName_isTheModuleArtifactId() {
        assertEquals("kudos-ability-security-common", SecurityCommonAutoConfiguration().getComponentName())
    }

    @Configuration(proxyBeanMethods = false)
    open class CustomBeansConfiguration {

        @Bean
        open fun customPasswordEncoder(): PasswordEncoder = ENCODER

        @Bean
        open fun customAuthenticator(): Authenticator = AUTHENTICATOR

        companion object {
            val ENCODER: PasswordEncoder = object : PasswordEncoder {
                override fun encode(rawPassword: CharSequence?): String = "plain:$rawPassword"
                override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean =
                    encodedPassword == "plain:$rawPassword"
            }

            val AUTHENTICATOR: Authenticator = object : Authenticator {
                override fun generateKey(): String = "FAKEKEY"
                override fun verify(secret: String, code: Int): Boolean = code == 0
                override fun generateCode(secret: String): String = "000000"
            }
        }
    }
}

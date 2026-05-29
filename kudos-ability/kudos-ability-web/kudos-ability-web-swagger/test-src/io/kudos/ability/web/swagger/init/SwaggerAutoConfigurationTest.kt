package io.kudos.ability.web.swagger.init

import io.kudos.ability.web.swagger.init.properties.SwaggerProperties
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Conditional-configuration tests for [SwaggerAutoConfiguration] using Spring Boot's
 * [ApplicationContextRunner]. The runner spins up a minimal context per test, registers only the
 * config-under-test, and lets us assert what beans exist + their wired state without booting a
 * full `@SpringBootApplication`.
 *
 * Coverage:
 *  - Default activation: with the module's bundled `kudos-ability-web-swagger.yml` on the
 *    classpath, an [OpenAPI] bean is published whose `info` block reflects the yml defaults
 *    (title=`kudos`, version=`1.0.0`, etc.).
 *  - Property override: yml-style overrides via `withPropertyValues(...)` bind to
 *    [SwaggerProperties] and propagate into the [Info] block.
 *  - `enabled=false` short-circuits the whole config — neither [SwaggerProperties] nor [OpenAPI]
 *    are registered. (The `@ConditionalOnProperty` covers the class itself.)
 *  - `@ConditionalOnMissingBean` allows a user-defined [OpenAPI] to take precedence; the
 *    module-provided default does not overwrite it.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SwaggerAutoConfigurationTest {

    private val runner: ApplicationContextRunner = ApplicationContextRunner()
        .withUserConfiguration(SwaggerAutoConfiguration::class.java)

    @Test
    fun defaults_publishesOpenAPIWithBundledYmlValues() {
        runner.run { ctx ->
            assertNotNull(ctx.getBean(SwaggerProperties::class.java))
            val openAPI = ctx.getBean(OpenAPI::class.java)
            val info = openAPI.info
            assertNotNull(info, "default OpenAPI bean must carry an info block populated from the bundled yml")
            assertEquals("kudos", info.title, "title comes from the module-default kudos-ability-web-swagger.yml")
            assertEquals("1.0.0", info.version)
            assertEquals("kudos OpenAPI documentation", info.description)
        }
    }

    @Test
    fun propertyOverrides_propagateIntoInfoBlock() {
        runner
            .withPropertyValues(
                "kudos.ability.web.swagger.title=My App",
                "kudos.ability.web.swagger.version=2.5.0",
                "kudos.ability.web.swagger.description=Custom description",
                "kudos.ability.web.swagger.contact.contact-name=Alice",
                "kudos.ability.web.swagger.contact.contact-email=alice@example.com",
            )
            .run { ctx ->
                val info = ctx.getBean(OpenAPI::class.java).info
                assertEquals("My App", info.title)
                assertEquals("2.5.0", info.version)
                assertEquals("Custom description", info.description)
                assertEquals("Alice", info.contact.name)
                assertEquals("alice@example.com", info.contact.email)
            }
    }

    @Test
    fun enabledFalse_skipsEntireConfiguration() {
        runner
            .withPropertyValues("kudos.ability.web.swagger.enabled=false")
            .run { ctx ->
                assertEquals(
                    0, ctx.getBeanNamesForType(OpenAPI::class.java).size,
                    "ConditionalOnProperty(enabled=false) must short-circuit OpenAPI bean creation",
                )
                assertEquals(
                    0, ctx.getBeanNamesForType(SwaggerProperties::class.java).size,
                    "the @Configuration class itself is gated, so SwaggerProperties never binds",
                )
            }
    }

    @Test
    fun userDefinedOpenAPI_takesPrecedence() {
        val userOpenAPI = OpenAPI().info(Info().title("from user config").version("9.9.9"))
        runner
            .withBean(OpenAPI::class.java, { userOpenAPI })
            .run { ctx ->
                val beans = ctx.getBeansOfType(OpenAPI::class.java)
                assertEquals(1, beans.size, "ConditionalOnMissingBean must back off when a user OpenAPI is present")
                assertSame(userOpenAPI, beans.values.single(), "the user-provided OpenAPI must remain the wired bean")
                assertEquals("from user config", ctx.getBean(OpenAPI::class.java).info.title)
            }
    }

    @Test
    fun productionFlag_isReadable_butDoesNotGateBean() {
        // production is a marker for downstream filters; toggling it must NOT suppress the OpenAPI
        // bean (which would break build-time SDK codegen on production-profile builds).
        runner
            .withPropertyValues("kudos.ability.web.swagger.production=true")
            .run { ctx ->
                val props = ctx.getBean(SwaggerProperties::class.java)
                assertEquals(true, props.production)
                assertNotNull(ctx.getBean(OpenAPI::class.java), "production=true must not suppress the OpenAPI bean")
            }
    }

}

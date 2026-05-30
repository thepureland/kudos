package io.kudos.ability.security.jwt.resourceserver.init

import io.kudos.ability.security.jwt.resourceserver.init.properties.JwtResourceServerProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Conditional-wiring tests for [JwtResourceServerAutoConfiguration] using
 * [ApplicationContextRunner].
 *
 * Scope: we verify the conditional gates and property binding. The actual `SecurityFilterChain`
 * wiring depends on Spring Security's `HttpSecurity` infrastructure, which lives in
 * `spring-boot-starter-web`'s security autoconfig path and is non-trivial to set up in a
 * lightweight ApplicationContextRunner. Real apps using this module exercise the filter chain
 * end-to-end via their own `@SpringBootTest` MVC tests — that's the right boundary.
 *
 * What we cover here:
 *  - The autoconfig class loads without exception (smoke).
 *  - `JwtResourceServerProperties` binds correctly from yml: `enabled`, `permittedPaths`.
 *  - The opt-in gate is respected: `enabled=false` (default) keeps the @Configuration class out
 *    of the bean graph; `enabled=true` lets it on.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class JwtResourceServerAutoConfigurationTest {

    private val runner: ApplicationContextRunner = ApplicationContextRunner()
        .withUserConfiguration(JwtResourceServerAutoConfiguration::class.java)

    @Test
    fun defaultDisabled_propertiesBeanIsNotEvenRegistered() {
        // When enabled=false (or absent) the @Configuration class is skipped wholesale by the
        // class-level @ConditionalOnProperty — so even the bound properties bean stays absent.
        runner.run { ctx ->
            assertEquals(
                0, ctx.getBeanNamesForType(JwtResourceServerProperties::class.java).size,
                "module is opt-in; @ConditionalOnProperty(enabled=true) on the class itself must " +
                    "keep the properties bean off the bus when the flag is absent",
            )
        }
    }

    @Test
    fun enabledTrue_propertiesBeanIsRegistered_withDefaults() {
        runner
            .withPropertyValues("kudos.ability.security.jwt.resource-server.enabled=true")
            .run { ctx ->
                val props = ctx.getBean(JwtResourceServerProperties::class.java)
                assertNotNull(props, "JwtResourceServerProperties must bind when enabled=true")
                assertTrue(props.enabled, "enabled must reflect the property value")
                assertTrue(
                    props.permittedPaths.isEmpty(),
                    "permittedPaths must default to empty (most restrictive default)",
                )
            }
    }

    @Test
    fun enabledTrue_permittedPathsList_bindsFromYml() {
        runner
            .withPropertyValues(
                "kudos.ability.security.jwt.resource-server.enabled=true",
                "kudos.ability.security.jwt.resource-server.permitted-paths[0]=/actuator/health",
                "kudos.ability.security.jwt.resource-server.permitted-paths[1]=/api/public/**",
                "kudos.ability.security.jwt.resource-server.permitted-paths[2]=/login/**",
            )
            .run { ctx ->
                val props = ctx.getBean(JwtResourceServerProperties::class.java)
                assertEquals(
                    listOf("/actuator/health", "/api/public/**", "/login/**"),
                    props.permittedPaths,
                    "permittedPaths must bind as an ordered list — Spring uses indexed yaml shape",
                )
            }
    }
}

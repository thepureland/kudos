package io.kudos.ms.auth.api.admin.dev

import io.kudos.ms.auth.api.admin.init.AuthApiAdminApplication
import io.kudos.test.container.containers.H2TestContainer
import io.kudos.test.container.containers.RedisTestContainer
import io.kudos.ms.auth.core.principal.spi.ISubjectResolver
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Boots the auth admin backend for real, with shadow mode on — run via `gradlew shadowDemo`.
 *
 * **Why this exists.** Every enforcement layer in this module is designed to be adopted through
 * shadow mode: switch it on, let real traffic produce the work-list, act on the list, then enforce.
 * Until something actually runs that way, the whole migration story is a claim. This launcher is the
 * smallest thing that makes it a fact: the real application, the real filter chain, the real
 * startup-time permission-point registration — fed by real HTTP from kudos-console-ui, whose vite
 * proxy points at :8080.
 *
 * **Why it lives in test scope.** It reuses the same containers the test suite runs on (H2 on its
 * fixed 1521, Redis on a random mapped port), and those helpers are test-classpath by design. It is
 * a dev tool, not a deployment artifact — a production deployment supplies its own datasource and
 * would never boot through testcontainers.
 *
 * The static half of the configuration lives in `test-resources/application.yml`; the only thing
 * this main() knows that the yml cannot is the Redis port, which does not exist until the container
 * starts.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object ShadowModeDemoLauncher

/**
 * Contributes the dev-only header resolver, so the demo can exercise the *positive* enforcement
 * path — a named subject, a real decision, a request that actually gets through. Registered here
 * rather than as a `@Component` so it can only ever exist in this launcher's context.
 */
@Configuration(proxyBeanMethods = false)
open class ShadowModeDemoConfiguration {
    @Bean
    open fun devHeaderSubjectResolver(): ISubjectResolver = DevHeaderSubjectResolver()
}

fun main() {
    // Containers are shared with any concurrently running tests (same labels, same cross-process
    // lock), so a `shadowDemo` alongside a test run reuses instead of colliding.
    System.setProperty("testcontainers.ryuk.disabled", "true")
    H2TestContainer.startIfNeeded(null)
    val redis = RedisTestContainer.startIfNeeded(null)

    val port = redis.ports.first()
    val redisHost = requireNotNull(port.ip) { "redis container reported no host" }
    val redisPort = requireNotNull(port.publicPort) { "redis container reported no public port" }
    // System properties outrank the yml, which is the whole trick: everything knowable in advance
    // is declared there, and only the value that is *born at runtime* is injected here.
    System.setProperty("kudos.ability.data.redis.redis-map.data.host", redisHost)
    System.setProperty("kudos.ability.data.redis.redis-map.data.port", redisPort.toString())
    System.setProperty("spring.data.redis.host", redisHost)
    System.setProperty("spring.data.redis.port", redisPort.toString())

    SpringApplication.run(
        arrayOf(AuthApiAdminApplication::class.java, ShadowModeDemoConfiguration::class.java),
        emptyArray(),
    )
}

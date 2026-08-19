package io.kudos.ability.distributed.client.http.init

import org.springframework.boot.SpringApplication
import org.springframework.core.Ordered
import org.springframework.core.env.MapPropertySource
import org.springframework.mock.env.MockEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [HttpClientDefaultsEnvironmentPostProcessor].
 *
 * The property it supplies is not a tuning preference — leaving Resilience4j's thread pool enabled
 * makes every outbound call run off the caller's thread, which silently drops the whole `KudosContext`
 * (tenant id included). So both halves are worth pinning: that the default is applied, and that an
 * application can still override it deliberately.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class HttpClientDefaultsEnvironmentPostProcessorTest {

    private val processor = HttpClientDefaultsEnvironmentPostProcessor()

    @Test
    fun appliesTheThreadPoolDefault() {
        val environment = MockEnvironment()

        processor.postProcessEnvironment(environment, SpringApplication())

        assertEquals(
            "true",
            environment.getProperty(DISABLE_THREAD_POOL),
            "context propagation depends on the call staying on the caller's thread"
        )
    }

    @Test
    fun anExplicitSettingWins() {
        // addLast means "default", not "override": an application that knowingly wants the hard
        // total-call timeout back must be able to ask for it.
        val environment = MockEnvironment().apply {
            propertySources.addFirst(MapPropertySource("app", mapOf(DISABLE_THREAD_POOL to "false")))
        }

        processor.postProcessEnvironment(environment, SpringApplication())

        assertEquals("false", environment.getProperty(DISABLE_THREAD_POOL))
    }

    @Test
    fun registersItsSourceLast() {
        val environment = MockEnvironment()

        processor.postProcessEnvironment(environment, SpringApplication())

        val names = environment.propertySources.map { it.name }
        assertEquals(
            HttpClientDefaultsEnvironmentPostProcessor.PROPERTY_SOURCE_NAME,
            names.last(),
            "defaults must sit behind every other source"
        )
        assertTrue(processor.order == Ordered.LOWEST_PRECEDENCE)
    }

    private companion object {
        const val DISABLE_THREAD_POOL = "spring.cloud.circuitbreaker.resilience4j.disable-thread-pool"
    }
}

package io.kudos.ability.data.tsdb.influxdb.init

import com.influxdb.client.InfluxDBClient
import io.kudos.ability.data.tsdb.influxdb.init.properties.InfluxdbProperties
import org.mockito.Mockito
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Conditional-wiring tests for [InfluxdbAutoConfiguration] using Spring Boot's
 * [ApplicationContextRunner].
 *
 * No real InfluxDB is contacted — the autoconfig builds an [InfluxDBClient] via the InfluxDB
 * Java client factory which creates an OkHttp-backed client lazily, so just constructing the
 * bean doesn't trigger a network call. We only assert on bean presence and identity.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class InfluxdbAutoConfigurationTest {

    private val runner: ApplicationContextRunner = ApplicationContextRunner()
        .withUserConfiguration(InfluxdbAutoConfiguration::class.java)

    @Test
    fun noUrlNoToken_clientIsNotRegistered() {
        runner.run { ctx ->
            assertEquals(
                0, ctx.getBeanNamesForType(InfluxDBClient::class.java).size,
                "InfluxDBClient must not wire when url + token are absent — defensive default for " +
                    "apps that pull the module in without a live server",
            )
        }
    }

    @Test
    fun onlyUrlSet_clientIsStillNotRegistered() {
        runner
            .withPropertyValues("kudos.ability.tsdb.influxdb.url=http://localhost:8086")
            .run { ctx ->
                assertEquals(
                    0, ctx.getBeanNamesForType(InfluxDBClient::class.java).size,
                    "missing token → no client; partial config must not leave a half-authenticated client lying around",
                )
            }
    }

    @Test
    fun onlyTokenSet_clientIsStillNotRegistered() {
        runner
            .withPropertyValues("kudos.ability.tsdb.influxdb.token=secret-token")
            .run { ctx ->
                assertEquals(
                    0, ctx.getBeanNamesForType(InfluxDBClient::class.java).size,
                    "missing url → no client",
                )
            }
    }

    @Test
    fun urlAndTokenSet_clientIsWired() {
        runner
            .withPropertyValues(
                "kudos.ability.tsdb.influxdb.url=http://localhost:8086",
                "kudos.ability.tsdb.influxdb.token=secret-token",
                "kudos.ability.tsdb.influxdb.org=my-org",
                "kudos.ability.tsdb.influxdb.bucket=metrics",
            )
            .run { ctx ->
                val client = ctx.getBean(InfluxDBClient::class.java)
                assertNotNull(client, "InfluxDBClient must be wired when url + token are both present")
                // The InfluxDB Java client returns a `BlockingInfluxDBClient` or similar; we just
                // care that we got *something* implementing InfluxDBClient.
                assertTrue(
                    InfluxDBClient::class.java.isAssignableFrom(client.javaClass),
                    "wired bean must implement InfluxDBClient; got ${client.javaClass}",
                )
                val props = ctx.getBean(InfluxdbProperties::class.java)
                assertEquals("http://localhost:8086", props.url)
                assertEquals("secret-token", props.token)
                assertEquals("my-org", props.org)
                assertEquals("metrics", props.bucket)
            }
    }

    @Test
    fun userProvidedClient_takesPrecedenceOverAutoConfig() {
        // Apps that need OkHttp tuning or a wrapped client declare their own bean; the autoconfig
        // must step aside via @ConditionalOnMissingBean. Using a Mockito mock keeps the test from
        // depending on the InfluxDB Java client's concrete classes (which vary by version).
        val userClient = Mockito.mock(InfluxDBClient::class.java)
        runner
            .withPropertyValues(
                "kudos.ability.tsdb.influxdb.url=http://localhost:8086",
                "kudos.ability.tsdb.influxdb.token=secret-token",
            )
            .withBean(InfluxDBClient::class.java, { userClient })
            .run { ctx ->
                val beans = ctx.getBeansOfType(InfluxDBClient::class.java)
                assertEquals(1, beans.size, "ConditionalOnMissingBean must keep the autoconfig client off the bus")
                assertSame(userClient, beans.values.single(), "user-provided client must remain the wired bean")
            }
    }
}

package io.kudos.ability.data.tsdb.influxdb.init

import com.influxdb.LogLevel
import com.influxdb.client.InfluxDBClient
import io.kudos.ability.data.tsdb.influxdb.init.properties.InfluxdbProperties
import org.mockito.Mockito
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
    fun minimalUrlAndTokenOnly_clientIsWired_optionalPropsStayNull() {
        // Covers the null-branches of `org?.let`, `bucket?.let`, `logLevel?.let` in the bean
        // factory — proves the builder tolerates a minimal config without optional fields.
        runner
            .withPropertyValues(
                "kudos.ability.tsdb.influxdb.url=http://localhost:8086",
                "kudos.ability.tsdb.influxdb.token=secret-token",
            )
            .run { ctx ->
                assertNotNull(
                    ctx.getBean(InfluxDBClient::class.java),
                    "client must wire with only url + token — org/bucket/logLevel are optional",
                )
                val props = ctx.getBean(InfluxdbProperties::class.java)
                assertNull(props.org, "org must stay null when unset")
                assertNull(props.bucket, "bucket must stay null when unset")
                assertNull(props.logLevel, "logLevel must stay null when unset")
                assertNull(props.readTimeout, "readTimeout must stay null when unset")
                assertNull(props.writeTimeout, "writeTimeout must stay null when unset")
            }
    }

    @Test
    fun fullConfigWithLogLevelAndTimeouts_clientIsWired_andAllPropsBind() {
        // Covers the non-null branch of `logLevel?.let` plus Duration binding of the two
        // timeout fields (kept on the properties class for forward binary compatibility even
        // though the autoconfig doesn't yet push them into OkHttp).
        runner
            .withPropertyValues(
                "kudos.ability.tsdb.influxdb.url=http://localhost:8086",
                "kudos.ability.tsdb.influxdb.token=secret-token",
                "kudos.ability.tsdb.influxdb.org=my-org",
                "kudos.ability.tsdb.influxdb.bucket=metrics",
                "kudos.ability.tsdb.influxdb.log-level=BODY",
                "kudos.ability.tsdb.influxdb.read-timeout=45s",
                "kudos.ability.tsdb.influxdb.write-timeout=PT1M30S",
            )
            .run { ctx ->
                assertNotNull(
                    ctx.getBean(InfluxDBClient::class.java),
                    "client must wire when logLevel is set — exercises the logLevel?.let branch",
                )
                val props = ctx.getBean(InfluxdbProperties::class.java)
                assertEquals(LogLevel.BODY, props.logLevel, "log-level must bind to the InfluxDB LogLevel enum")
                assertEquals(Duration.ofSeconds(45), props.readTimeout, "read-timeout must bind simple-style durations")
                assertEquals(Duration.ofSeconds(90), props.writeTimeout, "write-timeout must bind ISO-8601 durations")
            }
    }

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals(
            "kudos-ability-data-tsdb-influxdb",
            InfluxdbAutoConfiguration().getComponentName(),
            "component name drives the kudos initializer banner and must match the module id",
        )
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

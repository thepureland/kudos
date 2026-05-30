package io.kudos.ability.data.tsdb.influxdb

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.InfluxdbTestContainer
import jakarta.annotation.Resource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end integration test for [io.kudos.ability.data.tsdb.influxdb.init.InfluxdbAutoConfiguration]
 * against a real InfluxDB 2.7 server provisioned by [InfluxdbTestContainer].
 *
 * Verifies the chain works at runtime against a live server:
 *  - the autoconfig wires [InfluxDBClient] off the testcontainer-registered url/token,
 *  - a [Point] written through the sync write API round-trips through a Flux query,
 *  - per-measurement filtering works (proves the bucket/org binding is correct),
 *  - the `InfluxDBClient.ping()` health hook succeeds (proves the auth token + URL are usable
 *    end-to-end, not just typecheck-correct).
 *
 * Why end-to-end (not just unit): the InfluxDB Java client constructs its OkHttp client lazily,
 * so the `InfluxdbAutoConfigurationTest` ApplicationContextRunner test could verify bean wiring
 * without ever hitting the network. This test closes that gap — guards against future regressions
 * like a wrong default precision, a missing org/bucket propagation, or a token-handling bug.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
internal class InfluxdbIntegrationTest {

    @Resource
    private lateinit var influx: InfluxDBClient

    @Test
    fun client_isWired_andHealthCheckPasses() {
        assertNotNull(influx, "InfluxDBClient must be autowired by InfluxdbAutoConfiguration")
        val health = influx.ping()
        assertTrue(health, "InfluxDB /health must respond OK — proves URL + token are valid end-to-end")
    }

    @Test
    fun writeAPI_thenQuery_roundTripsAPoint() {
        // Unique measurement per test run so concurrent test runs / cross-test bleed don't break us.
        val measurement = "kudos-it-${UUID.randomUUID().toString().take(8)}"
        val now = Instant.now()
        val point = Point.measurement(measurement)
            .addTag("host", "test-host-1")
            .addField("usage", 0.42)
            .time(now, WritePrecision.MS)

        influx.writeApiBlocking.writePoint(point)

        val flux = """
            from(bucket: "${InfluxdbTestContainer.BUCKET}")
              |> range(start: -1m)
              |> filter(fn: (r) => r._measurement == "$measurement")
        """
        val tables = influx.queryApi.query(flux, InfluxdbTestContainer.ORG)
        // Tables grouped by tag set; we wrote one point so we expect exactly one record back.
        val records = tables.flatMap { it.records }
        assertEquals(
            1, records.size,
            "exactly one record must come back for the just-written measurement; got ${records.size}",
        )
        val record = records.single()
        assertEquals("usage", record.field, "field name must round-trip")
        assertEquals(0.42, record.value, "field value must round-trip with default MS precision")
        assertEquals("test-host-1", record.getValueByKey("host"), "tag value must round-trip")
    }

    @Test
    fun query_filtersByMeasurement_excludesOthers() {
        // Defensive: prove that a second measurement written to the same bucket is NOT picked up
        // by a filter on the first measurement. Guards against accidental cross-measurement leakage
        // if a future refactor changes the Flux filter semantics.
        val mA = "it-a-${UUID.randomUUID().toString().take(8)}"
        val mB = "it-b-${UUID.randomUUID().toString().take(8)}"
        influx.writeApiBlocking.writePoint(
            Point.measurement(mA).addField("v", 1.0).time(Instant.now(), WritePrecision.MS),
        )
        influx.writeApiBlocking.writePoint(
            Point.measurement(mB).addField("v", 2.0).time(Instant.now(), WritePrecision.MS),
        )

        val flux = """
            from(bucket: "${InfluxdbTestContainer.BUCKET}")
              |> range(start: -1m)
              |> filter(fn: (r) => r._measurement == "$mA")
        """
        val records = influx.queryApi.query(flux, InfluxdbTestContainer.ORG).flatMap { it.records }

        assertEquals(1, records.size, "filter on measurement A must not pick up the row written for B")
        assertEquals(1.0, records.single().value)
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            InfluxdbTestContainer.startIfNeeded(registry)
        }
    }
}

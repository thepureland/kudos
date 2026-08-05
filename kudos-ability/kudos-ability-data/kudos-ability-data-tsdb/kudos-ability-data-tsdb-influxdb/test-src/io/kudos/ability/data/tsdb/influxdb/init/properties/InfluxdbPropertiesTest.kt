package io.kudos.ability.data.tsdb.influxdb.init.properties

import com.influxdb.LogLevel
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit tests for [InfluxdbProperties].
 *
 * Covers: all-null defaults (the "module on classpath, no server provisioned" posture the KDoc
 * promises), every mutable accessor pair round-tripping (including the two timeout fields that
 * the autoconfig doesn't yet consume but keeps for binary compatibility), Unicode/edge values,
 * and resetting back to null.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class InfluxdbPropertiesTest {

    @Test
    fun defaults_areAllNull() {
        val props = InfluxdbProperties()
        assertNull(props.url, "url must default to null so the autoconfig backs off")
        assertNull(props.token, "token must default to null so the autoconfig backs off")
        assertNull(props.org)
        assertNull(props.bucket)
        assertNull(props.logLevel, "null logLevel → InfluxDB client default (NONE)")
        assertNull(props.readTimeout, "null readTimeout → InfluxDB client default (10s)")
        assertNull(props.writeTimeout, "null writeTimeout → InfluxDB client default (10s)")
    }

    @Test
    fun allAccessors_roundTrip() {
        val props = InfluxdbProperties()
        props.url = "http://localhost:8086"
        props.token = "secret-token"
        props.org = "my-org"
        props.bucket = "metrics"
        props.logLevel = LogLevel.HEADERS
        props.readTimeout = Duration.ofSeconds(30)
        props.writeTimeout = Duration.ofMillis(2500)

        assertEquals("http://localhost:8086", props.url)
        assertEquals("secret-token", props.token)
        assertEquals("my-org", props.org)
        assertEquals("metrics", props.bucket)
        assertEquals(LogLevel.HEADERS, props.logLevel)
        assertEquals(Duration.ofSeconds(30), props.readTimeout)
        assertEquals(Duration.ofMillis(2500), props.writeTimeout)
    }

    @Test
    fun accessors_acceptEdgeValues_andResetToNull() {
        val props = InfluxdbProperties()

        // Empty strings are legal at the property-holder level — the autoconfig's
        // @ConditionalOnProperty gate is what rejects blank config, not this class.
        props.url = ""
        props.token = ""
        assertEquals("", props.url)
        assertEquals("", props.token)

        // Unicode org/bucket names and zero/negative durations must store verbatim — the
        // holder performs no validation by design.
        props.org = "组织-δοκιμή"
        props.bucket = "桶 bucket"
        props.readTimeout = Duration.ZERO
        props.writeTimeout = Duration.ofSeconds(-1)
        assertEquals("组织-δοκιμή", props.org)
        assertEquals("桶 bucket", props.bucket)
        assertEquals(Duration.ZERO, props.readTimeout)
        assertEquals(Duration.ofSeconds(-1), props.writeTimeout)

        // Every field can be reset to null (e.g. profile-specific unbinding).
        props.url = null
        props.token = null
        props.org = null
        props.bucket = null
        props.logLevel = null
        props.readTimeout = null
        props.writeTimeout = null
        assertNull(props.url)
        assertNull(props.token)
        assertNull(props.org)
        assertNull(props.bucket)
        assertNull(props.logLevel)
        assertNull(props.readTimeout)
        assertNull(props.writeTimeout)
    }
}

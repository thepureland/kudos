package io.kudos.ability.data.tsdb.influxdb.init.properties

import com.influxdb.LogLevel
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for the kudos-ability-data-tsdb-influxdb module.
 *
 * Bound from `kudos.ability.tsdb.influxdb.*` in `application.yml`. The `url` + `token` pair is
 * the minimum viable config — when either is blank the autoconfig backs off and no
 * `InfluxDBClient` bean is registered, so depending on this module without yet provisioning the
 * server doesn't blow up context refresh.
 *
 * `org` and `bucket` aren't `@ConditionalOnProperty`-gating because callers may want to override
 * them per write/query call (different buckets for different metric families).
 *
 * Ported from soul's `InfluxdbProperties`. Soul additionally shipped an `InfluxdbMultiProperties`
 * for multi-data-source routing across different InfluxDB instances — that's deliberately not
 * ported in this MVP (single-source covers the common monitoring/IoT use case; multi-source can
 * be a follow-up sub-module if an app demands it).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.tsdb.influxdb")
class InfluxdbProperties {

    /**
     * HTTP base URL of the InfluxDB 2.x server, e.g. `http://localhost:8086`. Blank/missing
     * disables the autoconfig so apps can pull this module into their bundle without forcing a
     * live InfluxDB in every profile.
     */
    var url: String? = null

    /**
     * Auth token. Required in InfluxDB 2.x — there is no anonymous mode. Blank disables the
     * autoconfig the same way [url] does.
     */
    var token: String? = null

    /** Default organization name (`-o` in the influx CLI). Apps can override per call. */
    var org: String? = null

    /** Default bucket name (`-b` in the influx CLI). Apps can override per call. */
    var bucket: String? = null

    /**
     * OkHttp request/response logging level. Maps directly to InfluxDB Java client's
     * `LogLevel` enum. Default null → InfluxDB client default ([LogLevel.NONE]).
     */
    var logLevel: LogLevel? = null

    /**
     * OkHttp read timeout. Default null → InfluxDB client default (10s). Apps that run long
     * Flux queries (multi-hour windows) should bump this; reflects the wall-clock budget for a
     * single query response.
     */
    var readTimeout: Duration? = null

    /**
     * OkHttp write timeout. Default null → InfluxDB client default (10s). Apps that ingest very
     * large point batches per write call should bump this; the write side defaults are
     * conservative.
     */
    var writeTimeout: Duration? = null
}

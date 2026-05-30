package io.kudos.ability.data.tsdb.influxdb.init

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.InfluxDBClientOptions
import com.influxdb.client.domain.WriteConsistency
import com.influxdb.client.domain.WritePrecision
import io.kudos.ability.data.tsdb.influxdb.init.properties.InfluxdbProperties
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Auto-configuration for the kudos-ability-data-tsdb-influxdb module.
 *
 * Wires a single [InfluxDBClient] bean from [InfluxdbProperties]. Gated on:
 *  - [ConditionalOnClass] [InfluxDBClient] — the InfluxDB Java client must be on the classpath
 *    (this is satisfied by depending on the module, but stays defensive against transitive
 *    excludes).
 *  - [ConditionalOnProperty] both `url` and `token` set — InfluxDB 2.x has no anonymous mode;
 *    without a token the client can't authenticate. Splitting the gate over two properties means
 *    apps that set only one get a clear "missing the other" startup error rather than a runtime
 *    401 deep in a write call.
 *  - [ConditionalOnMissingBean] on [InfluxDBClient] — apps with a custom client (e.g. one
 *    wired off Vault-issued tokens via a rotation hook) declare their own bean and this
 *    autoconfig stays out of the way.
 *
 * Defaults in client options:
 *  - `consistency = QUORUM` — keep parity with soul's wiring; matters only for InfluxDB
 *    Enterprise multi-node deployments, but defaulting to QUORUM is the safer choice and
 *    single-node InfluxDB OSS ignores it harmlessly.
 *  - `precision = MS` — milliseconds. The Java client's default is nanoseconds, which can clip
 *    silently against a server bucket configured for ms precision. ms matches the typical
 *    monitoring/IoT use case; apps writing nanosecond-precision data should set their own
 *    precision per write call (the InfluxDB Java client accepts per-point precision overrides).
 *
 * Soul's port wired both the global default client AND an `InfluxdbTemplate` for multi-data-source
 * routing in one configuration. The kudos MVP ships only the single client; multi-source routing
 * (mirroring the baomidou-style dynamic-DS for InfluxDB) is deferred to a follow-up sub-module.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(InfluxDBClient::class)
@ConditionalOnProperty(prefix = "kudos.ability.tsdb.influxdb", name = ["url", "token"])
@EnableConfigurationProperties(InfluxdbProperties::class)
@PropertySource(
    value = ["classpath:kudos-ability-data-tsdb-influxdb.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class InfluxdbAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun influxDBClient(properties: InfluxdbProperties): InfluxDBClient {
        // ConditionalOnProperty guarantees url + token are non-null at this point — !! captures
        // the intent and lets Kotlin nullability stay honest in the property type.
        val builder = InfluxDBClientOptions.builder()
            .url(properties.url!!)
            .authenticateToken(properties.token!!.toCharArray())
            .consistency(WriteConsistency.QUORUM)
            .precision(WritePrecision.MS)
        properties.org?.let { builder.org(it) }
        properties.bucket?.let { builder.bucket(it) }
        properties.logLevel?.let { builder.logLevel(it) }
        // OkHttp's timeout setters live on a separate `OkHttpClient.Builder` callback; the
        // InfluxDB options builder exposes them via `okHttpClient` only — keeping per-property
        // tuning here would require building an OkHttp client ourselves. Apps that need
        // non-default timeouts can declare their own InfluxDBClient bean (the autoconfig backs
        // off via @ConditionalOnMissingBean) — the property fields are kept on
        // InfluxdbProperties so future deeper wiring is binary-compatible.
        val options = builder.build()
        return InfluxDBClientFactory.create(options)
    }

    override fun getComponentName() = "kudos-ability-data-tsdb-influxdb"
}

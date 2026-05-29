package io.kudos.ability.data.docdb.mongo.init

import io.kudos.ability.data.docdb.mongo.convert.BigIntegerConverters
import io.kudos.ability.data.docdb.mongo.init.properties.MongoCustomProperties
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

/**
 * Auto-configuration for the kudos-ability-data-docdb-mongo module.
 *
 * MVP scope:
 *  - Registers a [MongoCustomConversions] bean carrying [BigIntegerConverters] when the toggle is
 *    enabled. Spring Boot's `MongoAutoConfiguration` picks the bean up automatically and wires it
 *    into the [MongoTemplate] it builds from `spring.data.mongodb.*` — no need to redefine the
 *    template.
 *  - `@Primary` so an app's later-loaded "vanilla" `MongoCustomConversions` (e.g. via a different
 *    starter) doesn't accidentally win over the BigInteger-aware one.
 *
 * Deliberately NOT in MVP (deferred to follow-up commit when an actual app needs it):
 *  - Dynamic data source routing across multiple Mongo instances
 *    (soul's `MongoDynamicRoutingTemplate` + `MongoDynamicDataSourceAspect`)
 *  - Custom connection-pool config (soul's `MongoCustomProperties.PoolConfig`) — Spring Boot
 *    already exposes `spring.data.mongodb.connection-pool-size` / `max-wait-time` etc.
 *
 * Gating:
 *  - [ConditionalOnClass] on [MongoTemplate]: only activates when spring-data-mongodb is on the
 *    classpath, so a transitive depender doesn't get this config when they didn't intend to.
 *  - [ConditionalOnProperty] `kudos.ability.docdb.mongo.big-integer-as-string` (default true):
 *    apps can opt out without excluding the module dependency.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MongoTemplate::class)
@EnableConfigurationProperties(MongoCustomProperties::class)
@PropertySource(
    value = ["classpath:kudos-ability-data-docdb-mongo.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class MongoAutoConfiguration : IComponentInitializer {

    @Bean
    @Primary
    @ConditionalOnProperty(
        prefix = "kudos.ability.docdb.mongo",
        name = ["big-integer-as-string"],
        havingValue = "true",
        matchIfMissing = true,
    )
    open fun mongoCustomConversions(): MongoCustomConversions =
        MongoCustomConversions(
            listOf(
                BigIntegerConverters.BigIntegerToString,
                BigIntegerConverters.StringToBigInteger,
            ),
        )

    override fun getComponentName() = "kudos-ability-data-docdb-mongo"
}

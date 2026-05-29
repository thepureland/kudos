package io.kudos.ability.web.guest.init

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.web.guest.filter.GuestAccessFilter
import io.kudos.ability.web.guest.init.properties.GuestProperties
import io.kudos.ability.web.guest.provider.GuestAccessService
import io.kudos.ability.web.guest.provider.GuestAccessUniqueKey
import io.kudos.ability.web.guest.provider.IGuestAccessIgnore
import io.kudos.ability.web.guest.provider.IGuestAccessListener
import io.kudos.ability.web.guest.provider.IGuestAccessService
import io.kudos.ability.web.guest.provider.IGuestAccessStore
import io.kudos.ability.web.guest.provider.IGuestAccessUniqueKey
import io.kudos.ability.web.guest.provider.RedisGuestAccessStore
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Wires the guest-tracking SPIs + the servlet filter that runs them.
 *
 * Every default bean is gated by [ConditionalOnMissingBean] so an app can replace any SPI
 * (custom store, custom unique-key, custom service) without forking the module.
 *
 * The filter bean is always registered when this auto-config loads — the per-request gate lives
 * inside [GuestAccessFilter.doFilterInternal] (consulting [GuestProperties.enabled]), not at the
 * bean-creation layer. This is deliberate: it lets [GuestProperties.enabled] flip at runtime via
 * config-server push without tearing down the Spring context.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GuestProperties::class)
@PropertySource(
    value = ["classpath:kudos-ability-web-guest.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class GuestAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun guestAccessUniqueKey(properties: GuestProperties): IGuestAccessUniqueKey =
        GuestAccessUniqueKey(properties)

    @Bean
    @ConditionalOnMissingBean
    open fun guestAccessService(
        properties: GuestProperties,
        uniqueKey: IGuestAccessUniqueKey,
        ignores: ObjectProvider<IGuestAccessIgnore>,
    ): IGuestAccessService = GuestAccessService(properties, uniqueKey, ignores)

    @Bean
    @ConditionalOnMissingBean
    open fun guestAccessStore(
        properties: GuestProperties,
        redisTemplates: RedisTemplates,
        listenerProvider: ObjectProvider<IGuestAccessListener>,
    ): IGuestAccessStore = RedisGuestAccessStore(properties, redisTemplates, listenerProvider.ifAvailable)

    @Bean
    @ConditionalOnMissingBean
    open fun guestAccessFilter(
        service: IGuestAccessService,
        store: IGuestAccessStore,
    ): GuestAccessFilter = GuestAccessFilter(service, store)

    override fun getComponentName() = "kudos-ability-web-guest"
}

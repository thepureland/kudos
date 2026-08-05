package io.kudos.ability.data.memdb.redis.init

import io.kudos.ability.data.memdb.redis.RedisConnectFactory
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.kudos.ability.data.memdb.redis.init.properties.RedisProperties
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Role
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate


/**
 * Redis auto-configuration class.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@PropertySource(
    value = ["classpath:kudos-ability-data-memdb-redis.yml"],
    factory = YamlPropertySourceFactory::class
)
@AutoConfigureAfter(ContextAutoConfiguration::class)
// See ContextAutoConfiguration: IComponentInitializer configuration classes must be instantiated before business BPPs;
// ROLE_INFRASTRUCTURE is added to avoid false warnings from Spring's BeanPostProcessorChecker.
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
open class RedisAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.data.redis")
    open fun redisProperties(): RedisProperties = RedisProperties()

    @Bean(name = ["kudosRedisTemplate"])
    @ConditionalOnMissingBean
    open fun redisTemplateMap(
        redisProperties: RedisProperties
    ): RedisTemplates {
        val redisMap = redisProperties.redisMap
        val redisTemplateMap = mutableMapOf<String, RedisTemplate<Any, Any?>>()
        redisMap.forEach { (key, properties) ->
            val lettuceConnectionFactory = RedisConnectFactory.newLettuceConnectionFactory(properties)
            // createRedisTemplate already invokes afterPropertiesSet() on the template.
            redisTemplateMap[key] = createRedisTemplate(lettuceConnectionFactory, properties)
        }

        val defaultKey = requireNotNull(redisProperties.defaultRedis) { "kudos.ability.data.redis.default-redis must be set" }
        val defaultRedisTemplate = requireNotNull(redisTemplateMap[defaultKey]) { "no redis config for default-redis: $defaultKey" }
        return RedisTemplates(redisTemplateMap, defaultRedisTemplate)
    }

    @Bean("redisTemplate")
    @ConditionalOnMissingBean
    open fun redisTemplate(redisTemplateMap: RedisTemplates): RedisTemplate<Any, Any?> =
        redisTemplateMap.defaultRedisTemplate

    /**
     * Creates a redisTemplate from the given Redis connection factory.
     *
     * @param redisConnectionFactory redisConnectionFactory
     * @return RedisTemplate<Any, Any?>
     */
    private fun createRedisTemplate(
        redisConnectionFactory: RedisConnectionFactory,
        redisProperties: RedisExtProperties,
    ): RedisTemplate<Any, Any?> {
        val redisTemplate = RedisTemplate<Any, Any?>()
        redisTemplate.connectionFactory = redisConnectionFactory
        redisTemplate.keySerializer = redisProperties.keySerializer()
        redisTemplate.hashKeySerializer = redisProperties.hashkeySerializer()
        redisTemplate.valueSerializer = redisProperties.valueSerializer()
        redisTemplate.hashValueSerializer = redisProperties.hashvalueSerializer()
        redisTemplate.afterPropertiesSet()
        return redisTemplate
    }

    override fun getComponentName() = "kudos-ability-data-memdb-redis"

}

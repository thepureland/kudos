package io.kudos.ability.data.memdb.redis.init

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.RedisConnectFactory
import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.kudos.ability.data.memdb.redis.init.properties.RedisProperties
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate


/**
 * Redis自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@PropertySource(
    value = ["classpath:kudos-ability-data-memdb-redis.yml"],
    factory = YamlPropertySourceFactory::class
)
@AutoConfigureAfter(ContextAutoConfiguration::class)
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
            val redisTemplate = createRedisTemplate(lettuceConnectionFactory, properties)
            redisTemplate.afterPropertiesSet()
            redisTemplateMap[key] = redisTemplate
        }

        //拼装KudosRedisTemplate
        val defaultRedisTemplate = redisTemplateMap[redisProperties.defaultRedis]!!
        val redisTemplates = RedisTemplates(redisTemplateMap, defaultRedisTemplate)
        return redisTemplates
    }

    @Bean("redisTemplate")
    @ConditionalOnMissingBean
    open fun redisTemplate(redisTemplateMap: RedisTemplates): RedisTemplate<Any, Any?> {
        return redisTemplateMap.defaultRedisTemplate
    }

    /**
     * 根据redis连接工厂创建redisTemplate
     *
     * @param redisConnectionFactory redisConnectionFactory
     * @return RedisTemplate<String></String>, Object>
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
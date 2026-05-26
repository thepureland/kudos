package io.kudos.ability.comm.sms.aliyun.init

import io.kudos.ability.comm.sms.aliyun.handler.AliyunSmsHandler
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


/**
 * Aliyun SMS sending auto-configuration class.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-comm-sms-aliyun.yml"],
    factory = YamlPropertySourceFactory::class
)
open class AliyunSmsAutoConfiguration : IComponentInitializer {

    /**
     * Aliyun SMS send handler. The endpoint defaults to empty (uses the SDK's built-in region
     * domain); for test / Mock scenarios, override via `kudos.ability.comm.sms.aliyun.endpoint`
     * (see the field annotation on [AliyunSmsHandler]).
     */
    @Bean
    @ConditionalOnMissingBean
    open fun aliyunSmsHandler() = AliyunSmsHandler()

    override fun getComponentName() = "kudos-ability-comm-sms-aliyun"

}
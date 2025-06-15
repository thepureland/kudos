package io.kudos.ability.comm.sms.aws.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.spring.YamlPropertySourceFactory
import org.soul.ability.comm.sms.aws.handler.AwsSmsHandler
import org.soul.ability.comm.sms.aws.starter.properties.SmsAwsProxyProperties
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource


/**
 * aws短信发送自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configurable
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-comm-sms-aws.yml"],
    factory = YamlPropertySourceFactory::class
)
open class AwsSmsAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun aliyunSmsHandler() = AwsSmsHandler()

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.comm.sms.aws.proxy")
    open fun smsAwsProxyProperties() = SmsAwsProxyProperties()

    override fun getComponentName() = "kudos-ability-comm-sms-aws"

}
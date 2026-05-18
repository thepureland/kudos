package io.kudos.ability.comm.sms.aws.init

import io.kudos.ability.comm.sms.aws.handler.AwsSmsHandler
import io.kudos.ability.comm.sms.aws.init.properties.SmsAwsProxyProperties
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


/**
 * aws短信发送自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-comm-sms-aws.yml"],
    factory = YamlPropertySourceFactory::class
)
open class AwsSmsAutoConfiguration : IComponentInitializer {

    /**
     * AWS SNS 短信发送 handler。Endpoint 默认空（走 SDK 内置 region 域名）；测试场景
     * 通过 `kudos.ability.comm.sms.aws.endpoint` 覆盖为 WireMock 等地址。
     */
    @Bean
    @ConditionalOnMissingBean
    open fun awsSmsHandler() = AwsSmsHandler()

    /** AWS SDK 正向代理配置；`enable=false` 时不构造共享 HTTP 客户端，SDK 自带网络默认。 */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.comm.sms.aws.proxy")
    open fun smsAwsProxyProperties() = SmsAwsProxyProperties()

    override fun getComponentName() = "kudos-ability-comm-sms-aws"

}
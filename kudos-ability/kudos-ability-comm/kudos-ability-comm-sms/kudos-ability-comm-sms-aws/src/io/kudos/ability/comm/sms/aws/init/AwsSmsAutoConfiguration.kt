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
 * AWS SMS sending auto-configuration class.
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
     * AWS SNS SMS send handler. The endpoint defaults to empty (uses the SDK's built-in region
     * domain); for test scenarios, override via `kudos.ability.comm.sms.aws.endpoint` to point at
     * WireMock or similar.
     */
    @Bean
    @ConditionalOnMissingBean
    open fun awsSmsHandler() = AwsSmsHandler()

    /** AWS SDK forward proxy configuration; when `enable=false`, no shared HTTP client is built and the SDK uses its default networking. */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.comm.sms.aws.proxy")
    open fun smsAwsProxyProperties() = SmsAwsProxyProperties()

    override fun getComponentName() = "kudos-ability-comm-sms-aws"

}
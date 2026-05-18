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
 * aliyun短信发送自动配置类
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
     * 阿里云短信发送 handler。Endpoint 默认空（走 SDK 内置 region 域名）；测试 / Mock 场景
     * 通过 `kudos.ability.comm.sms.aliyun.endpoint` 覆盖（见 [AliyunSmsHandler] 的字段注解）。
     */
    @Bean
    @ConditionalOnMissingBean
    open fun aliyunSmsHandler() = AliyunSmsHandler()

    override fun getComponentName() = "kudos-ability-comm-sms-aliyun"

}
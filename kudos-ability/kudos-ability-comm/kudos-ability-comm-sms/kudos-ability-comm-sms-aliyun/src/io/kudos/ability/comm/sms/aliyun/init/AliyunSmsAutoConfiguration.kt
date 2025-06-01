package io.kudos.ability.comm.sms.aliyun.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.comm.sms.aliyun.handler.AliyunSmsHandler
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean


/**
 * aliyun短信发送自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configurable
@AutoConfigureAfter(ContextAutoConfiguration::class)
open class AliyunSmsAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun aliyunSmsHandler() = AliyunSmsHandler()

    override fun getComponentName() = "kudos-ability-comm-sms-aliyun"

}
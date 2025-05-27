package io.kudos.ability.comm.sms.aliyun.init

import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import org.soul.ability.comm.sms.aliyun.handler.AliyunSmsHandler
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import javax.annotation.PostConstruct


/**
 * aliyun短信发送自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configurable
open class AliyunSmsAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean
    open fun aliyunSmsHandler() = AliyunSmsHandler()


    @PostConstruct
    override fun init() {
        LoggerFactory.getLogger(this).info("【kudos-ability-comm-sms-aliyun】初始化完成.")
    }

}
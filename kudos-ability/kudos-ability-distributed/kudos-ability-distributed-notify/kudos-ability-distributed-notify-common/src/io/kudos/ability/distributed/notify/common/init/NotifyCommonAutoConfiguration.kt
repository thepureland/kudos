package io.kudos.ability.distributed.notify.common.init

import io.kudos.ability.distributed.notify.common.support.NotifyListenerBeanPostProcessor
import io.kudos.ability.distributed.notify.common.support.NotifyTool
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean


/**
 * notify公共自动配置类
 *
 * @author K
 * @since 1.0.0
 */
open class NotifyCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    open fun notifyListenerBeanPostProcessor() = NotifyListenerBeanPostProcessor()

    @Bean
    @ConditionalOnMissingBean
    open fun notifyTool() = NotifyTool()

}
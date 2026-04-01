package io.kudos.ability.distributed.notify.common.init

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.support.NotifyListenerBeanPostProcessor
import io.kudos.ability.distributed.notify.common.support.NotifyListenerItem
import io.kudos.ability.distributed.notify.common.support.NotifyTool
import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment


/**
 * notify公共自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@PropertySource(
    value = ["classpath:kudos-ability-distributed-notify-common.yml"],
    factory = YamlPropertySourceFactory::class
)
open class NotifyCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.notify")
    open fun notifyCommonProperties() = NotifyCommonProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun notifyListenerBeanPostProcessor(
        properties: NotifyCommonProperties,
        environment: Environment
    ): NotifyListenerBeanPostProcessor {
        val namespace = properties.listenerNamespace
            ?.takeIf { it.isNotBlank() }
            ?: environment.getProperty("spring.application.name")
            ?: NotifyListenerItem.DEFAULT_NAMESPACE
        return NotifyListenerBeanPostProcessor(namespace)
    }

    @Bean
    @ConditionalOnMissingBean
    open fun notifyTool(
        notifyProducerProvider: ObjectProvider<INotifyProducer>,
        properties: NotifyCommonProperties
    ) = NotifyTool(notifyProducerProvider.ifAvailable, properties)

    @Bean
    @ConditionalOnMissingBean(name = ["notifyProducerAvailabilityVerifier"])
    open fun notifyProducerAvailabilityVerifier(
        notifyProducerProvider: ObjectProvider<INotifyProducer>,
        properties: NotifyCommonProperties
    ): InitializingBean = InitializingBean {
        if (properties.failOnMissingProducer && notifyProducerProvider.ifAvailable == null) {
            error("未找到INotifyProducer实现，且已开启kudos.ability.distributed.notify.fail-on-missing-producer")
        }
    }

}
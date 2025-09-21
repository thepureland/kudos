package io.kudos.ability.distributed.notify.mq.init

import com.alibaba.fastjson.JSONObject
import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.NotifyCommonAutoConfiguration
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyListenerBeanPostProcessor
import io.kudos.ability.distributed.notify.common.support.NotifyListenerItem
import io.kudos.ability.distributed.notify.mq.producer.NotifyMqProducer
import io.kudos.ability.distributed.stream.common.annotations.MqConsumer
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.base.logger.LogFactory
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import org.springframework.messaging.Message
import java.util.function.Consumer


/**
 * 基于MQ的通知的自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = [
        "classpath:kudos-ability-distributed-notify-mq-consumer.yml",
        "classpath:kudos-ability-distributed-notify-mq-producer.yml"
    ],
    factory = YamlPropertySourceFactory::class
)
@Import(NotifyListenerBeanPostProcessor::class)
open class NotifyMqAutoConfiguration : NotifyCommonAutoConfiguration(), IComponentInitializer {

    private val log = LogFactory.getLog(this)

    @Bean(name = [INotifyProducer.BEAN_NAME])
    @ConditionalOnMissingBean
    open fun notifyMqProducer(): INotifyProducer = NotifyMqProducer()

    @MqConsumer(topic = "mqNotify", bindingName = "mqNotify-in-0", beanName = ["mqNotify"])
    open fun mqNotify(): Consumer<Message<StreamMessageVo<JSONObject>>?> {
        return Consumer { msg: Message<StreamMessageVo<JSONObject>>? ->
            val streamMsgVo = msg!!.getPayload()
            val socketMsgJson = streamMsgVo.data!!
            val simpleMsgVo = socketMsgJson.toJavaObject(NotifyMessageVo::class.java)
            log.info("[mqNotify]消费通知, 类型:${simpleMsgVo.notifyType}")
            val listener = NotifyListenerItem.get(simpleMsgVo.notifyType)
            if (listener != null) {
                listener.notifyProcess(simpleMsgVo)
            } else {
                log.info("[mqNotify] 类型:${simpleMsgVo.notifyType}, 无 listener 配置")
            }
        }
    }

    override fun getComponentName() = "kudos-ability-distributed-notify-mq"

}
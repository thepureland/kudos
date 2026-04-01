package io.kudos.ability.distributed.notify.mq.init

import com.alibaba.fastjson2.JSONObject
import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.NotifyCommonAutoConfiguration
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyListenerItem
import io.kudos.ability.distributed.notify.mq.producer.NotifyMqProducer
import io.kudos.ability.distributed.stream.common.annotations.MqConsumer
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.base.logger.LogFactory
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
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
open class NotifyMqAutoConfiguration : NotifyCommonAutoConfiguration(), IComponentInitializer {

    private val log = LogFactory.getLog(this::class)

    @Autowired(required = false)
    private var notifyCommonProperties: NotifyCommonProperties? = null

    @Autowired(required = false)
    private var environment: Environment? = null

    @Bean(name = [INotifyProducer.BEAN_NAME])
    @ConditionalOnMissingBean
    open fun notifyMqProducer(): INotifyProducer = NotifyMqProducer()

    @Bean
    @ConditionalOnMissingBean(name = ["notifyMqProducerBindingVerifier"])
    open fun notifyMqProducerBindingVerifier(
        bindingServicePropertiesProvider: ObjectProvider<BindingServiceProperties>
    ): InitializingBean = InitializingBean {
        val bindingName = "mqNotify-out-0"
        val bindingProps = bindingServicePropertiesProvider.ifAvailable?.bindings
        if (bindingProps.isNullOrEmpty() || !bindingProps.containsKey(bindingName)) {
            log.warn("[notify-mq] 未找到Stream生产者binding配置: {0}，通知发送可能不可用", bindingName)
        }
    }

    @MqConsumer(topic = "mqNotify", bindingName = "mqNotify-in-0", beanName = ["mqNotify"])
    open fun mqNotify(): Consumer<Message<StreamMessageVo<JSONObject>>?> = Consumer { msg ->
        val streamMsgVo = msg?.payload ?: run {
            log.warn("[mqNotify] 收到空消息，忽略")
            return@Consumer
        }

        val socketMsgJson = streamMsgVo.data ?: run {
            log.warn("[mqNotify] 收到空 data，忽略")
            return@Consumer
        }

        val simpleMsgVo = runCatching { socketMsgJson.toJavaObject(NotifyMessageVo::class.java) }
            .getOrElse {
                log.error(it, "[mqNotify] 通知消息反序列化失败")
                return@Consumer
            }

        val notifyType = simpleMsgVo.notifyType
        if (notifyType.isBlank()) {
            log.warn("[mqNotify] 通知类型为空，忽略")
            return@Consumer
        }

        log.info("[mqNotify] 消费通知, 类型: $notifyType")
        val namespace = resolveListenerNamespace()
        val listener = NotifyListenerItem.get(namespace, notifyType) ?: NotifyListenerItem.get(notifyType)
        listener?.notifyProcess(simpleMsgVo)
            ?: log.info("[mqNotify] 命名空间: $namespace, 类型: $notifyType, 无 listener 配置")
    }

    private fun resolveListenerNamespace(): String =
        notifyCommonProperties?.listenerNamespace
            ?.takeIf { it.isNotBlank() }
            ?: environment?.getProperty("spring.application.name")
            ?: NotifyListenerItem.DEFAULT_NAMESPACE

    override fun getComponentName() = "kudos-ability-distributed-notify-mq"

}
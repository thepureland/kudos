package io.kudos.ability.distributed.notify.mq.init

import com.alibaba.fastjson2.JSONObject
import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.init.NotifyCommonAutoConfiguration
import io.kudos.ability.distributed.notify.common.init.properties.NotifyCommonProperties
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyListenerItem
import io.kudos.ability.distributed.notify.mq.init.properties.NotifyMqProperties
import io.kudos.ability.distributed.notify.mq.producer.NotifyMqProducer
import io.kudos.ability.distributed.notify.mq.support.NotifyMqBindings
import io.kudos.ability.distributed.stream.common.annotations.MqConsumer
import io.kudos.ability.distributed.stream.common.annotations.MqProducerAspect
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
import org.springframework.boot.context.properties.ConfigurationProperties
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

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.notify.mq")
    open fun notifyMqProperties() = NotifyMqProperties()

    @Bean(name = [INotifyProducer.BEAN_NAME])
    @ConditionalOnMissingBean
    open fun notifyMqProducer(): INotifyProducer = NotifyMqProducer()

    @Bean
    @ConditionalOnMissingBean(name = ["notifyMqProducerBindingVerifier"])
    open fun notifyMqProducerBindingVerifier(
        notifyMqProperties: NotifyMqProperties,
        mqProducerAspectProvider: ObjectProvider<MqProducerAspect>,
        bindingServicePropertiesProvider: ObjectProvider<BindingServiceProperties>
    ): InitializingBean = InitializingBean {
        if (mqProducerAspectProvider.ifAvailable == null) {
            handleProducerVerifierFailure(
                notifyMqProperties,
                "[notify-mq] 未找到MqProducerAspect bean，NotifyMqProducer的AOP占位发送不会生效"
            )
        }
        val bindingName = NotifyMqBindings.PRODUCER_BINDING
        val bindingProps = bindingServicePropertiesProvider.ifAvailable?.bindings
        if (bindingProps.isNullOrEmpty() || !bindingProps.containsKey(bindingName)) {
            handleProducerVerifierFailure(
                notifyMqProperties,
                "[notify-mq] 未找到Stream生产者binding配置: $bindingName，通知发送可能不可用"
            )
        }
    }

    private fun handleProducerVerifierFailure(notifyMqProperties: NotifyMqProperties, message: String) {
        if (notifyMqProperties.failOnMissingProducerBinding) {
            error(message)
        } else {
            log.warn(message)
        }
    }

    @MqConsumer(
        topic = NotifyMqBindings.TOPIC,
        bindingName = NotifyMqBindings.CONSUMER_BINDING,
        beanName = [NotifyMqBindings.CONSUMER_BEAN]
    )
    open fun mqNotify(notifyMqProperties: NotifyMqProperties = NotifyMqProperties()): Consumer<Message<StreamMessageVo<JSONObject>>?> = Consumer { msg ->
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
                rethrowIfNeeded(notifyMqProperties, it)
                return@Consumer
            }

        val notifyType = simpleMsgVo.notifyType
        if (notifyType.isBlank()) {
            log.warn("[mqNotify] 通知类型为空，忽略")
            return@Consumer
        }

        log.info("[mqNotify] 消费通知, 类型: $notifyType")
        val namespace = resolveListenerNamespace()
        val listener = NotifyListenerItem.get(namespace, notifyType) ?: findDefaultNamespaceListener(namespace, notifyType)
        if (listener == null) {
            log.info("[mqNotify] 命名空间: $namespace, 类型: $notifyType, 无 listener 配置")
            return@Consumer
        }
        runCatching { listener.notifyProcess(simpleMsgVo) }
            .onFailure {
                log.error(it, "[mqNotify] listener处理失败, namespace: {0}, 类型: {1}", namespace, notifyType)
                rethrowIfNeeded(notifyMqProperties, it)
            }
    }

    private fun rethrowIfNeeded(notifyMqProperties: NotifyMqProperties, throwable: Throwable) {
        if (notifyMqProperties.rethrowConsumerException) {
            throw throwable
        }
    }

    private fun findDefaultNamespaceListener(namespace: String, notifyType: String) =
        if (notifyCommonProperties?.fallbackToDefaultNamespace == true &&
            namespace != NotifyListenerItem.DEFAULT_NAMESPACE
        ) {
            NotifyListenerItem.get(notifyType)
        } else {
            null
        }

    /**
     * 解析当前应用的 listener namespace。
     * 优先级：显式配置 `notifyCommonProperties.listenerNamespace` → `spring.application.name` → [NotifyListenerItem.DEFAULT_NAMESPACE]。
     * 让多应用同 MQ topic 时各自的 listener 仍能按 namespace 隔离。
     *
     * @return namespace 字符串
     * @author K
     * @since 1.0.0
     */
    private fun resolveListenerNamespace(): String =
        notifyCommonProperties?.listenerNamespace
            ?.takeIf { it.isNotBlank() }
            ?: environment?.getProperty("spring.application.name")
            ?: NotifyListenerItem.DEFAULT_NAMESPACE

    override fun getComponentName() = "kudos-ability-distributed-notify-mq"

}

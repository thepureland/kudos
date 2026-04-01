package io.kudos.ability.distributed.stream.common.init

import io.kudos.ability.distributed.stream.common.annotations.MqProducerAspect
import io.kudos.ability.distributed.stream.common.biz.ISysMqFailMsgService
import io.kudos.ability.distributed.stream.common.biz.SysMqFailMsgService
import io.kudos.ability.distributed.stream.common.dao.StreamExceptionMsgDao
import io.kudos.ability.distributed.stream.common.handler.IStreamFailHandler
import io.kudos.ability.distributed.stream.common.handler.StreamGlobalExceptionHandler
import io.kudos.ability.distributed.stream.common.handler.StreamProducerExceptionHandler
import io.kudos.ability.distributed.stream.common.init.properties.StreamBindingVerifyProperties
import io.kudos.ability.distributed.stream.common.support.StreamMessageConverter
import io.kudos.ability.distributed.stream.common.support.StreamProducerFailHandlerProcessor
import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.ability.distributed.stream.common.init.properties.StreamAsyncSendExecutorProperties
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.context.annotation.Bean
import org.springframework.integration.channel.DirectChannel
import org.springframework.messaging.MessageChannel
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Stream公共配置类
 *
 * @author K
 * @since 1.0.0
 */
open class StreamCommonConfiguration {

    private val log = LogFactory.getLog(this::class)

    @Bean
    @ConditionalOnMissingBean
    open fun streamAsyncSendExecutorProperties() = StreamAsyncSendExecutorProperties()

    @Bean
    @ConditionalOnMissingBean
    open fun streamBindingVerifyProperties() = StreamBindingVerifyProperties()

    @Bean("streamAsyncSendExecutor")
    @ConditionalOnMissingBean
    open fun streamAsyncSendExecutor(props: StreamAsyncSendExecutorProperties): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = props.corePoolSize
        executor.maxPoolSize = props.maxPoolSize
        executor.queueCapacity = props.queueCapacity
        executor.setThreadNamePrefix(props.threadNamePrefix)
        executor.initialize()
        return executor
    }

    @Bean
    @ConditionalOnMissingBean
    open fun mqProducerAspect() = MqProducerAspect()

    @Bean
    @ConditionalOnMissingBean
    open fun streamGlobalExceptionHandler() = StreamGlobalExceptionHandler()

    @Bean
    @ConditionalOnMissingBean
    open fun streamProducerExceptionHandler() = StreamProducerExceptionHandler()

    @Bean
    @ConditionalOnMissingBean
    open fun streamProducerFailHandlerProcessor() = StreamProducerFailHandlerProcessor()

    @Bean
    @ConditionalOnMissingBean
    open fun streamProducerHelper() = StreamProducerHelper()

    @Bean
    @ConditionalOnMissingBean
    open fun customMessageConverter() = StreamMessageConverter()

    @Bean
    @ConditionalOnMissingBean
    open fun streamExceptionBiz(streamExceptionMsgDao: StreamExceptionMsgDao): ISysMqFailMsgService =
        SysMqFailMsgService(streamExceptionMsgDao)

    @Bean
    @ConditionalOnMissingBean
    open fun streamExceptionMsgDao(): StreamExceptionMsgDao = StreamExceptionMsgDao()

    @Bean(IStreamFailHandler.CHANNEL_BEN_NAME)
    @ConditionalOnMissingBean
    open fun mqProducerChannel(): MessageChannel = DirectChannel()

    @Bean
    @ConditionalOnMissingBean(name = ["streamBindingVerifier"])
    open fun streamBindingVerifier(
        verifyProps: StreamBindingVerifyProperties,
        bindingPropsProvider: ObjectProvider<BindingServiceProperties>
    ): InitializingBean = InitializingBean {
        if (!verifyProps.enabled) {
            return@InitializingBean
        }
        val bindings = bindingPropsProvider.ifAvailable?.bindings?.keys ?: emptySet()
        val required = verifyProps.requiredProducerBindings.filter { it.isNotBlank() }
        if (required.isEmpty()) {
            log.warn("Stream binding自检已启用，但未配置requiredProducerBindings")
            return@InitializingBean
        }
        val missing = required.filter { !bindings.contains(it) }
        if (missing.isNotEmpty()) {
            val msg = "缺少必需的Stream producer bindings: $missing"
            if (verifyProps.failOnMissing) {
                error(msg)
            } else {
                log.warn(msg)
            }
        }
    }

}
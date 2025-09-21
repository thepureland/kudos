package io.kudos.ability.distributed.stream.common.init

import io.kudos.ability.distributed.stream.common.annotations.MqProducerAspect
import io.kudos.ability.distributed.stream.common.biz.IStreamExceptionBiz
import io.kudos.ability.distributed.stream.common.biz.StreamExceptionBiz
import io.kudos.ability.distributed.stream.common.dao.StreamExceptionMsgDao
import io.kudos.ability.distributed.stream.common.handler.IStreamFailHandler
import io.kudos.ability.distributed.stream.common.handler.StreamGlobalExceptionHandler
import io.kudos.ability.distributed.stream.common.handler.StreamProducerExceptionHandler
import io.kudos.ability.distributed.stream.common.support.StreamMessageConverter
import io.kudos.ability.distributed.stream.common.support.StreamProducerFailHandlerProcessor
import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
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

    @Bean("streamAsyncSendExecutor")
    @ConditionalOnMissingBean
    open fun streamAsyncSendExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 32 // 设置核心线程数
        executor.maxPoolSize = 128 // 设置最大线程数
        executor.queueCapacity = 1024 // 设置队列大小
        executor.threadNamePrefix = "stream-async-" // 设置线程名前缀
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
    open fun streamExceptionBiz(): IStreamExceptionBiz = StreamExceptionBiz()

    @Bean
    @ConditionalOnMissingBean
    open fun streamExceptionMsgDao(): StreamExceptionMsgDao = StreamExceptionMsgDao()

    @Bean(IStreamFailHandler.CHANNEL_BEN_NAME)
    @ConditionalOnMissingBean
    open fun mqProducerChannel(): MessageChannel = DirectChannel()

}
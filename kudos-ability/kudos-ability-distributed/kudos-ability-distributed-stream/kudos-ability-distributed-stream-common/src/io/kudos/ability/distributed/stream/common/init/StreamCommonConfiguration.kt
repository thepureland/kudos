package io.kudos.ability.distributed.stream.common.init

import io.kudos.ability.distributed.stream.common.annotations.MqProducerAspect
import org.mybatis.spring.annotation.MapperScan
import org.soul.ability.distributed.stream.common.handler.StreamGlobalExceptionHandler
import org.soul.ability.distributed.stream.common.iservice.IStreamExceptionService
import org.soul.ability.distributed.stream.common.service.StreamExceptionService
import org.soul.ability.distributed.stream.common.support.SoulStreamMessageConverter
import org.soul.ability.distributed.stream.common.support.StreamProducerHelper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Stream公共配置类
 *
 * @author K
 * @since 1.0.0
 */
@MapperScan("org.soul.ability.distributed.stream.common.data")
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
    open fun streamProducerHelper() = StreamProducerHelper()

    @Bean
    @ConditionalOnMissingBean
    open fun customMessageConverter() = SoulStreamMessageConverter()

    @Bean
    @ConditionalOnMissingBean
    open fun streamExceptionService(): IStreamExceptionService = StreamExceptionService()

}
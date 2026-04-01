package io.kudos.ability.distributed.stream.common.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Stream 异步发送线程池（[io.kudos.ability.distributed.stream.common.init.StreamCommonConfiguration.streamAsyncSendExecutor]）配置。
 *
 * 绑定前缀：`kudos.ability.distributed.stream.async-executor`
 */
@ConfigurationProperties(prefix = "kudos.ability.distributed.stream.async-executor")
open class StreamAsyncSendExecutorProperties {

    var corePoolSize: Int = 32

    var maxPoolSize: Int = 128

    var queueCapacity: Int = 1024

    var threadNamePrefix: String = "stream-async-"
}

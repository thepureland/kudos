package io.kudos.ability.comm.common.init.properties

class CommThreadPoolProperties {
    /**
     * 线程名称前缀,默认comm-pool
     */
    var threadNamePrefix: String? = "comm-pool"

    /**
     * 线程池维护线程的最少数量,默认为3
     */
    var corePoolSize: Int? = 3

    /**
     * 线程池维护线程的最大数量,默认为10
     */
    var maxPoolSize: Int? = 10

    /**
     * 线程池维护线程所允许的空闲时间,默认为900秒
     */
    var keepAliveSeconds: Int? = 900

    /**
     * 线程池所使用的缓冲队列,默认为100
     */
    var queueCapacity: Int? = 100
}

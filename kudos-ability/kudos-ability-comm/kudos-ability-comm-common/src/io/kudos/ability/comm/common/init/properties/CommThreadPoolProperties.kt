package io.kudos.ability.comm.common.init.properties

/**
 * 通信类（邮件 / 短信 / 推送 / WebSocket）共享线程池配置。
 *
 * **注：当前**没有具体模块装这个配置（comm-email / comm-sms-* 都各自走虚拟线程）。
 * 保留是为未来需要"共享同步发送线程池"的场景预留。如果一直没用到，应当评估删除本模块。
 *
 * @author K
 * @since 1.0.0
 */
class CommThreadPoolProperties {
    /**
     * 线程名称前缀,默认comm-pool
     */
    var threadNamePrefix: String = "comm-pool"

    /**
     * 线程池维护线程的最少数量,默认为3
     */
    var corePoolSize: Int = 3

    /**
     * 线程池维护线程的最大数量,默认为10
     */
    var maxPoolSize: Int = 10

    /**
     * 线程池维护线程所允许的空闲时间,默认为900秒
     */
    var keepAliveSeconds: Int = 900

    /**
     * 线程池所使用的缓冲队列,默认为100
     */
    var queueCapacity: Int = 100
}

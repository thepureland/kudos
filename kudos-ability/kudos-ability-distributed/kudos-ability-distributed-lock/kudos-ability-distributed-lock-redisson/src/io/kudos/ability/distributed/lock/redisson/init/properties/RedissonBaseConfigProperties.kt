package io.kudos.ability.distributed.lock.redisson.init.properties

class RedissonBaseConfigProperties {
    /**
     * ping命令发送间隔，设置0为禁用，默认为0
     */
    var pingConnectionInterval: Int = 0

    /**
     * 连接空闲超时 如果当前连接池里的连接数量超过了最小空闲连接数，而同时有连接空闲时间超过了该数值，那么这些连接将会自动被关闭，并从连接池里去掉。时间单位是毫秒。
     */
    var idleConnectionTimeout: Int = 10000

    /**
     * 连接超时，单位：毫秒
     */
    var connectTimeout: Int = 10000

    /**
     * 命令等待超时，单位：毫秒
     */
    var timeout: Int = 3000

    /**
     * 命令失败重试次数,如果尝试达到 retryAttempts（命令失败重试次数） 仍然不能将命令发送至某个指定的节点时，将抛出错误。
     * 如果尝试在此限制之内发送成功，则开始启用 timeout（命令等待超时） 计时。
     */
    var retryAttempts: Int = 3

    /**
     * 命令重试发送时间间隔，单位：毫秒
     */
    var retryInterval: Int = 1500

    /**
     * 密码
     */
    var password: String? = null

    /**
     * 单个连接最大订阅数量
     */
    var subscriptionsPerConnection: Int = 5

    /**
     * 客户端名称
     */
    var clientName: String? = ""

    /**
     * 发布和订阅连接的最小空闲连接数
     */
    var subscriptionConnectionMinimumIdleSize: Int = 1

    /**
     * 发布和订阅连接池大小
     */
    var subscriptionConnectionPoolSize: Int = 50

    /**
     * DNS监测时间间隔，单位：毫秒
     */
    var dnsMonitoringInterval: Long = 5000L
}

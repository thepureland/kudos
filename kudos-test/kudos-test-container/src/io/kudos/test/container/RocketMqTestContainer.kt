package io.kudos.test.container

import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.soul.base.net.IpTool
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer
import java.util.function.Supplier

object RocketMqTestContainer {
    private val LOG: Log? = LogFactory.getLog(RocketMqTestContainer::class.java)
    private const val IMAGE_NAME = "apache/rocketmq:"
    private const val IMAGE_DASHBORD = "apacherocketmq/rocketmq-dashboard:1.0.0"

    // 注意rocketmq 5.0.0 以上版本，有时会发生topic不会自动建立问题，即使有配置 autoCreateTopicEnable=true，也会发生。
    private const val IMAGE_VERSION = "4.9.7"
    private val IMAGE = IMAGE_NAME + IMAGE_VERSION

    const val PORT: Int = 9876
    private val LOCAL_IP: String? = IpTool.getLocalIp()
    val host: String = LOCAL_IP + ":" + PORT
    private val BROKER_CONF_PATH: String = "/home/rocketmq/rocketmq-%s/conf/broker.conf".formatted(IMAGE_VERSION)

    val nameSrv: FixedHostPortGenericContainer<*> = FixedHostPortGenericContainer<SELF?>(IMAGE)
        .withFixedExposedPort(PORT, 9876)
        .withEnv("MAX_HEAP_SIZE", "256M")
        .withEnv("HEAP_NEWSIZE", "128M")
        .withEnv("MAX_POSSIBLE_HEAP", "100000000")
        .withPrivilegedMode(true)
        .withCommand("sh mqnamesrv")

    val brokerSrv: FixedHostPortGenericContainer<*> = FixedHostPortGenericContainer<SELF?>(IMAGE)
        .withFixedExposedPort(10909, 10909)
        .withFixedExposedPort(10911, 10911)
        .withFixedExposedPort(10912, 10912)
        .withPrivilegedMode(true)
        .withEnv("NAMESRV_ADDR", host)
        .withEnv("MAX_POSSIBLE_HEAP", "200000000")
        .withEnv("MAX_HEAP_SIZE", "256M")
        .withCommand(
            "/bin/sh", "-c",
            "chmod 777 %s && echo \"brokerIP1=%s\nautoCreateTopicEnable=true\nconsumeBroadcastEnable=true\nconsumeEnable=true\" >> %s && sh mqbroker -c %s ".formatted(
                BROKER_CONF_PATH, LOCAL_IP, BROKER_CONF_PATH, BROKER_CONF_PATH
            )
        )

    private val DASHBORD: FixedHostPortGenericContainer<*>? = FixedHostPortGenericContainer<SELF?>(IMAGE_DASHBORD)
        .withFixedExposedPort(28080, 8080)
        .withPrivilegedMode(true)
        .withEnv("JAVA_OPTS", "-Drocketmq.namesrv.addr=" + host)

    fun start(registry: DynamicPropertyRegistry?) {
        nameSrv.start()
        brokerSrv.start()
        //        DASHBORD.start();
        if (registry != null) {
            registerProperties(registry)
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("soul.ability.distributed.stream.mq-config.rock-mq-name-server", Supplier { host })
    }

    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        System.out.printf("RocketMQ name-server localhost port: %s%n", PORT)
        System.out.printf("RocketMQ broker localhost ports: %s,%s,%s%n", 10909, 10911, 10912)
        Thread.sleep(Long.Companion.MAX_VALUE)
    }
}


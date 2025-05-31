package io.kudos.test.container

import io.kudos.base.net.IpKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.FixedHostPortGenericContainer

/**
 * rocket mq测试容器
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
object RocketMqTestContainer {

    private const val IMAGE_NAME = "apache/rocketmq:"
    private const val IMAGE_DASHBORD = "apacherocketmq/rocketmq-dashboard:1.0.0"

    // 注意rocketmq 5.0.0 以上版本，有时会发生topic不会自动建立问题，即使有配置 autoCreateTopicEnable=true，也会发生。
    private const val IMAGE_VERSION = "4.9.7"
    private val IMAGE = IMAGE_NAME + IMAGE_VERSION

    const val PORT = 9876
    private val LOCAL_IP = IpKit.getLocalIp()
    val NAMESRV_ADDR = "$LOCAL_IP:$PORT"
    private val BROKER_CONF_PATH = "/home/rocketmq/rocketmq-$IMAGE_VERSION/conf/broker.conf"

    val nameSrv = FixedHostPortGenericContainer(IMAGE)
        .withFixedExposedPort(PORT, 9876)
        .withEnv("MAX_HEAP_SIZE", "256M")
        .withEnv("HEAP_NEWSIZE", "128M")
        .withEnv("MAX_POSSIBLE_HEAP", "100000000")
        .withPrivilegedMode(true)
        .withCommand("sh mqnamesrv")

    val brokerSrv = FixedHostPortGenericContainer(IMAGE)
        .withFixedExposedPort(10909, 10909)
        .withFixedExposedPort(10911, 10911)
        .withFixedExposedPort(10912, 10912)
        .withPrivilegedMode(true)
        .withEnv("NAMESRV_ADDR", NAMESRV_ADDR)
        .withEnv("MAX_POSSIBLE_HEAP", "200000000")
        .withEnv("MAX_HEAP_SIZE", "256M")
        .withCommand(
            "/bin/sh", "-c",
            "chmod 777 $BROKER_CONF_PATH && echo \"brokerIP1=$LOCAL_IP\nautoCreateTopicEnable=true\nconsumeBroadcastEnable=true\nconsumeEnable=true\" >> $BROKER_CONF_PATH && sh mqbroker -c $BROKER_CONF_PATH "
        )

//    private val DASHBORD = FixedHostPortGenericContainer(IMAGE_DASHBORD)
//        .withFixedExposedPort(28080, 8080)
//        .withPrivilegedMode(true)
//        .withEnv("JAVA_OPTS", "-Drocketmq.namesrv.addr=$host")

    fun start(registry: DynamicPropertyRegistry?) {
        println(">>>>>>>>>>>>>>>>>>>> Starting RocketMQ container...")
        nameSrv.start()
        brokerSrv.start()
        //        DASHBORD.start();
        if (registry != null) {
            registerProperties(registry)
        }
        println(">>>>>>>>>>>>>>>>>>>> RocketMQ container started.")
    }

    private fun registerProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.cloud.stream.rocketmq.binder.name-server") { NAMESRV_ADDR }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start(null)
        println("RocketMQ name-server localhost port: $PORT", )
        println("RocketMQ broker localhost ports: 10909,10911,10912")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}


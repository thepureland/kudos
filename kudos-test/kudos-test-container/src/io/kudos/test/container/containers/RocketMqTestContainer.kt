package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.base.net.IpKit
import io.kudos.test.container.containers.H2TestContainer.LABEL
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer

/**
 * rocket mq测试容器
 *
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

    const val LABEL_NANE_SERVER = "RocketMQ name server"

    const val LABEL_BROKER_SERVER = "RocketMQ broker server"

    val nameServerContainer = GenericContainer(IMAGE).apply {
        withExposedPorts(PORT)
        bindingPort(Pair(PORT, PORT))
        withEnv("MAX_HEAP_SIZE", "256M")
        withEnv("HEAP_NEWSIZE", "128M")
        withEnv("MAX_POSSIBLE_HEAP", "100000000")
        withPrivilegedMode(true)
        withCommand("sh mqnamesrv")
        withLabel(TestContainerKit.LABEL_KEY, LABEL_NANE_SERVER)
    }

    private val brokerServerContainer = GenericContainer(IMAGE).apply {
        withExposedPorts(10909, 10911, 10912)
        bindingPort(Pair(10909, 10909), Pair(10911, 10911), Pair(10912, 10912))
        withPrivilegedMode(true)
        withEnv("NAMESRV_ADDR", NAMESRV_ADDR)
        withEnv("MAX_POSSIBLE_HEAP", "200000000")
        withEnv("MAX_HEAP_SIZE", "256M")
        withCommand(
            "/bin/sh", "-c",
            "chmod 777 $BROKER_CONF_PATH && echo \"brokerIP1=$LOCAL_IP\nautoCreateTopicEnable=true\nconsumeBroadcastEnable=true\nconsumeEnable=true\" >> $BROKER_CONF_PATH && sh mqbroker -c $BROKER_CONF_PATH "
        )
        withLabel(TestContainerKit.LABEL_KEY, LABEL_BROKER_SERVER)
    }

//    private val DASHBORD = FixedHostPortGenericContainer(IMAGE_DASHBORD)
//        .withFixedExposedPort(28080, 8080)
//        .withPrivilegedMode(true)
//        .withEnv("JAVA_OPTS", "-Drocketmq.namesrv.addr=$host")

    /**
     * 启动容器(若需要)。同时启动RocketMQ的name server和broker。
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没安装时想忽略测试用例，可以用@EnabledIfDockerInstalled
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Pair<Container, Container> {
        synchronized(this) {
            val runningNameServerContainer = TestContainerKit.startContainerIfNeeded(LABEL_NANE_SERVER, nameServerContainer)
            val runningBrokerServerContainer = TestContainerKit.startContainerIfNeeded(LABEL_BROKER_SERVER, brokerServerContainer)
            // DASHBORD.start();
            if (registry != null) {
                registerProperties(registry, runningNameServerContainer)
            }
            return Pair(runningNameServerContainer, runningBrokerServerContainer)
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry, runningNameServerContainer: Container) {
        registry.add("spring.cloud.stream.rocketmq.binder.name-server") { NAMESRV_ADDR }
    }

    /**
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null)
        println("RocketMQ name-server localhost port: $PORT")
        println("RocketMQ broker localhost ports: 10909,10911,10912")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}


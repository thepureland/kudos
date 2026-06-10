package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.base.net.IpKit
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer

/**
 * rocket mq test container.
 *
 * @author K
 * @since 1.0.0
 */
object RocketMqTestContainer {

    private const val IMAGE_NAME = "apache/rocketmq:"
    private const val IMAGE_DASHBORD = "apacherocketmq/rocketmq-dashboard:1.0.0"

    // Note: in RocketMQ versions >= 5.0.0, topics may sometimes fail to auto-create even with autoCreateTopicEnable=true configured.
    private const val IMAGE_VERSION = "4.9.7"
    private const val IMAGE = IMAGE_NAME + IMAGE_VERSION

    const val PORT = 9876
    private val LOCAL_IP = IpKit.getLocalIp()
    val NAMESRV_ADDR = "$LOCAL_IP:$PORT"
    private const val BROKER_CONF_PATH = "/home/rocketmq/rocketmq-$IMAGE_VERSION/conf/broker.conf"

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
     * Starts the containers (if needed). Starts both the RocketMQ name server and the broker.
     *
     * Ensures a single container is shared across a batch of tests, avoiding the time wasted starting/stopping containers repeatedly.
     * Alternatively, you can run this class's main method manually to start the container and share it while running tests.
     * Registers a JVM shutdown hook to automatically stop the container when the batch finishes,
     * rather than stopping after each test — provided the @Testcontainers annotation is not used.
     * To skip tests when Docker is not installed, use @EnabledIfDockerInstalled.
     *
     * @param registry Spring's dynamic property registry, used to register or override already-registered properties
     * @return the running container instances
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Pair<Container, Container> {
        return TestContainerCrossProcessLock.run(RocketMqTestContainer::class.java, "rocketmq") {
            val runningNameServerContainer = TestContainerKit.startContainerIfNeeded(LABEL_NANE_SERVER, nameServerContainer)
            val runningBrokerServerContainer = TestContainerKit.startContainerIfNeeded(LABEL_BROKER_SERVER, brokerServerContainer)
            // DASHBORD.start();
            if (registry != null) {
                registerProperties(registry, runningNameServerContainer)
            }
            Pair(runningNameServerContainer, runningBrokerServerContainer)
        }
    }

    /**
     * Registers the RocketMQ name-server address into Spring Cloud Stream configuration.
     * Uses the constant `NAMESRV_ADDR` directly rather than computing the port from `runningNameServerContainer` —
     * the container side already uses `bindingPort` to map a fixed PORT, avoiding port drift across startups.
     *
     * @param registry the Spring dynamic property registry
     * @param runningNameServerContainer the name-server container (parameter kept for future extensions using a dynamic port)
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningNameServerContainer: Container) {
        registry.add("spring.cloud.stream.rocketmq.binder.name-server") { NAMESRV_ADDR }
    }

    /**
     * Returns the running name-server container instance.
     *
     * Note: this used to (incorrectly) look up the H2 container's label via a stray static import,
     * which always returned the H2 container instead of the RocketMQ name server.
     *
     * @return the name-server container instance, or null if none is running
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL_NANE_SERVER)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL_NANE_SERVER, "RocketMQ name server")
        ManualTestContainerMainSupport.removeExistingContainers(LABEL_BROKER_SERVER, "RocketMQ broker server")
        startIfNeeded(null)
        println("RocketMQ name-server localhost port: $PORT")
        println("RocketMQ broker localhost ports: 10909,10911,10912")
        Thread.sleep(Long.MAX_VALUE)
    }

}


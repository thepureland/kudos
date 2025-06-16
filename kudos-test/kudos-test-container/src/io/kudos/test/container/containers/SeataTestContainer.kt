package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.base.net.IpKit
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * seata-server测试容器
 *
 * @author K
 * @since 1.0.0
 */
object SeataTestContainer {

    private const val IMAGE_NAME = "seataio/seata-server:2.0.0-slim"

    const val WEB_PORT = 27091

    const val SERVICE_PORT = 28091

    const val LABEL = "Seata"

    private lateinit var runningNacosContainer : Container

    private val CONTAINER = GenericContainer(IMAGE_NAME).apply {
        withExposedPorts(7091, 28091)
        bindingPort(Pair(WEB_PORT, 7091), Pair(SERVICE_PORT, 28091))
        withEnv("SEATA_IP", IpKit.getLocalIp())
        withNetwork(TestContainerKit.DEFAULT_DOCKER_NETWORK)
        withClasspathResourceMapping(
            "seata/seata-server.yml",
            "/seata-server/resources/application.yml",
            BindMode.READ_ONLY
        )
//        waitingFor(Wait.forHttp("/").forPort(7091))
        waitingFor(Wait.forListeningPort())
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * 启动容器(若需要)。同时启动Nacos和Seata，并使它们在同一docker网络，以便用别名访问。
     *
     * 保证批量测试时共享一个容器，避免多次开/停容器，浪费大量时间。
     * 另外，亦可手动运行该clazz类的main方法来启动容器，跑测试用例时共享它。
     * 并注册 JVM 关闭钩子，当批量测试结束时自动停止容器，
     * 而不是每个测试用例结束时就关闭，前提条件是不要加@Testcontainers注解。
     * 当docker没启动时想忽略测试用例，可以用@EnabledIfDockerAvailable
     * 来代替@Testcontainers(disabledWithoutDocker = true)
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        NacosTestContainer.startNacosForSeataIfNeeded(registry)

        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    private fun registerProperties(registry: DynamicPropertyRegistry?, runningContainer: Container) {
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
        println("nacos localhost web-port: ${runningNacosContainer.ports.first().publicPort}")
        println("seata localhost web-port: $WEB_PORT, service-port：$SERVICE_PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}

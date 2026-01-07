package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * nacos-server测试容器
 *
 * @author K
 * @since 1.0.0
 */
object NacosTestContainer {

    private const val IMAGE_NAME = "nacos/nacos-server:v3.1.1"

    const val PORT = 28848

    const val LABEL = "Nacos"

    const val LABEL_NACOS_FOR_SEATA = "Nacos（for Seata）"

    val tokenBytes = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }

    val tokenBase64 = java.util.Base64.getEncoder().encodeToString(tokenBytes)

    private val container = GenericContainer(IMAGE_NAME).apply {
        withEnv("MODE", "standalone")
        withEnv("PREFER_HOST_MODE", "ip")
        withEnv("NACOS_AUTH_ENABLE", "false")             // 不开启鉴权（3.x 默认基本就是要）
        withEnv("NACOS_AUTH_TOKEN", tokenBase64)  // ★ 必填：Base64 字符串
        withEnv("NACOS_AUTH_IDENTITY_KEY", "nacos")
        withEnv("NACOS_AUTH_IDENTITY_VALUE", "nacos")

        // 1. 声明容器要“暴露”的端口（必须和镜像 Dockerfile EXPOSE 一致）
        exposedPorts = listOf(8848, 9848, 9849)

        // 2. 再绑定宿主机端口 -> 容器端口
        bindingPort(Pair(PORT, 8848), Pair(29848, 9848), Pair(29849, 9849))

        // 3. 等待容器内的 8848 端口就绪
        waitingFor(Wait.forHttp("/nacos").forPort(8848))

        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    val containerForSeata = GenericContainer(IMAGE_NAME).apply {
        withEnv("MODE", "standalone")
        withEnv("NACOS_AUTH_ENABLE", "false")             // 开启鉴权（3.x 默认基本就是要）
        withEnv("NACOS_AUTH_TOKEN", tokenBase64)  // ★ 必填：Base64 字符串
        withEnv("NACOS_AUTH_IDENTITY_KEY", "nacos")
        withEnv("NACOS_AUTH_IDENTITY_VALUE", "nacos")
        withNetwork(TestContainerKit.DEFAULT_DOCKER_NETWORK)
        withNetworkAliases("nacos")
        exposedPorts = listOf(8848, 9848, 9849)
        bindingPort(Pair(38848, 8848), Pair(39848, 9848), Pair(39849, 9849))
        waitingFor(Wait.forHttp("/nacos").forPort(8848))
        withLabel(TestContainerKit.LABEL_KEY, LABEL_NACOS_FOR_SEATA)
    }


    /**
     * 启动容器(若需要)
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
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    fun startNacosForSeataIfNeeded(registry: DynamicPropertyRegistry?): Container {
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL_NACOS_FOR_SEATA, containerForSeata)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    internal fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
//        val host = runningContainer.ports.first().ip
        val host = "localhost"
//        val port = runningContainer.ports.first().publicPort
        val port = "38848"
        val serverAddr = "$host:$port"

        registry.add("spring.cloud.nacos.config.server-addr") { serverAddr }
        registry.add("spring.cloud.nacos.discovery.server-addr") { serverAddr }
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
        println("nacos localhost port: $PORT")
        Thread.sleep(Long.Companion.MAX_VALUE)
    }

}

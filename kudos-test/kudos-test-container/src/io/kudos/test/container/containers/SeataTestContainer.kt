package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.base.net.IpKit
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * seata-server测试容器
 *
 * @author K
 * @since 1.0.0
 */
object SeataTestContainer {

    private const val IMAGE_NAME = "apache/seata-server:2.5.0"

    const val WEB_PORT = 27091

    const val SERVICE_PORT = 28091

    const val LABEL = "Seata"

    /** 与 NacosTestContainer 中 Seata 用 Nacos 的宿主机端口一致 */
    private const val NACOS_FOR_SEATA_PORT = 38848

    private const val SEATA_SERVICE_NAME = "seata-server"

    /** 轮询间隔与最大等待时间，等 Seata 在 Nacos 注册后再返回，避免客户端 "no available service found in cluster" */
    private const val POLL_INTERVAL_MS = 800L
    private const val MAX_WAIT_MS = 30_000L

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
     * 当docker没安装时想忽略测试用例，可以用@EnabledIfDockerInstalled
     *
     * @param registry spring的动态属性注册器，可用来注册或覆盖已注册的属性
     * @return 运行中的容器对象
     */
    fun startIfNeeded(registry: DynamicPropertyRegistry?): Container {
        runningNacosContainer = NacosTestContainer.startNacosForSeataIfNeeded(registry)

        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, CONTAINER)
            if (registry != null) {
                registerProperties(registry, runningContainer)
                waitForSeataRegisteredInNacos()
            }
            return runningContainer
        }
    }

    /**
     * 测试 JVM 在宿主机上，若用 Nacos 发现会拿到 Seata 注册的地址（可能是容器内网 IP），宿主机连不上。
     * 这里覆盖为 file + 直连宿主机映射端口，仅测试侧直连 TC；Seata Server 仍用 Nacos+共用网络。
     */
    private fun registerProperties(registry: DynamicPropertyRegistry?, runningContainer: Container) {
        if (registry == null) return
        registry.add("seata.registry.type") { "file" }
        registry.add("seata.service.default.grouplist") { "127.0.0.1:$SERVICE_PORT" }
    }

    /**
     * 轮询 Nacos，直到 seata-server 已注册（至少一个实例）再返回，避免客户端启动时 "no available service found in cluster"。
     */
    private fun waitForSeataRegisteredInNacos() {
        val url = "http://127.0.0.1:$NACOS_FOR_SEATA_PORT/nacos/v1/ns/instance/list?serviceName=$SEATA_SERVICE_NAME"
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        val request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build()
        val deadline = System.currentTimeMillis() + MAX_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200 && response.body().contains("\"hosts\":[{")) {
                    return
                }
            } catch (_: Exception) {
                // Nacos 或 Seata 尚未就绪，继续轮询
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw IllegalStateException(
            "Seata server did not register to Nacos within ${MAX_WAIT_MS}ms. Check Nacos($NACOS_FOR_SEATA_PORT) and Seata container."
        )
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
        Thread.sleep(Long.MAX_VALUE)
    }

}

package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName


/**
 * WireMock测试容器
 *
 * @author K
 * @since 1.0.0
 */
object WireMockTestContainer {

    const val LABEL = "WireMock"

    private val container = GenericContainer(DockerImageName.parse("wiremock/wiremock:3.13.2-1-alpine"))
        .withExposedPorts(8080)
        .withCommand("--global-response-templating")
        .waitingFor(Wait.forHttp("/__admin").forStatusCode(200))
        .withLabel(TestContainerKit.LABEL_KEY, LABEL)


    /**
     * 启动容器(若需要)
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
        synchronized(this) {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            return runningContainer
        }
    }

    /**
     * WireMock 容器目前不注册任何 Spring 属性——测试代码会直接拿
     * [getRunningContainer] 拼出 baseUrl 自用，不需要透传到 Bean 配置层。
     * 保留方法是为了和其他 TestContainer 形态对齐 [startIfNeeded] 调用模板。
     *
     * @param registry Spring 动态属性注册表（允许 null）
     * @param runningContainer 运行中的容器
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry?, runningContainer: Container) {
    }

    /**
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer(): Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        startIfNeeded(null)
        println("WireMock localhost port: ${container.firstMappedPort}")
        Thread.sleep(Long.MAX_VALUE)
    }

}
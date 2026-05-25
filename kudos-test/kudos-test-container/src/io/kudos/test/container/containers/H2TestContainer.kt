package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.kit.bindingPort
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

/**
 * h2测试容器
 *
 * @author K
 * @since 1.0.0
 */
object H2TestContainer {

    /**
     * H2 测试镜像。
     *
     * 默认 `oscarfonts/h2:alpine` 只发布 amd64 manifest，在 Apple Silicon 等 arm64 主机上
     * 会触发 testcontainers 的架构不匹配警告并走 Rosetta/QEMU 模拟，启动较慢且偶发超时。
     * 可通过 `-Dkudos.test.h2.image=<image>` 或环境变量 `KUDOS_TEST_H2_IMAGE` 覆盖为
     * 本地构建或第三方多架构镜像（例如自建 `eclipse-temurin` + h2.jar 的镜像）。
     */
    private val IMAGE_NAME: String =
        System.getProperty("kudos.test.h2.image")
            ?: System.getenv("KUDOS_TEST_H2_IMAGE")
            ?: "oscarfonts/h2:alpine"

    const val DATABASE = "test"

    const val PORT = 1521

    const val USERNAME = "sa"

    const val PASSWORD = "sa"

    const val LABEL = "H2"

    private val container = GenericContainer(IMAGE_NAME).apply {
        // 通过环境变量指定 H2 启动参数：TCP 模式，允许外部访问，不存在时创建数据库
        withEnv("H2_OPTIONS", "-tcp -tcpAllowOthers -ifNotExists")
        withExposedPorts(PORT)
        bindingPort(Pair(PORT, PORT))
        waitingFor(Wait.forListeningPort())
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

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
        return TestContainerCrossProcessLock.run(H2TestContainer::class.java, "h2") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * 拼 H2 远程 in-memory JDBC URL（`DB_CLOSE_DELAY=-1` 防止最后一个连接断开就把内存库释放掉）
     * 并注册到 Spring 动态属性；用 `add` 让配置解析时再求值，避免容器未起就读到陈旧值。
     *
     * @param registry Spring 动态属性注册表
     * @param runningContainer 运行中的容器
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry, runningContainer : Container) {
        val host = runningContainer.ports.first().ip
        val port = runningContainer.ports.first().publicPort

        val url = "jdbc:h2:tcp://$host:$port/mem:$DATABASE;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;"
        registry.add("spring.datasource.dynamic.datasource.h2.url") { url }
        registry.add("spring.datasource.dynamic.datasource.h2.username") { USERNAME }
        registry.add("spring.datasource.dynamic.datasource.h2.password") { PASSWORD }
    }

    /**
     * 返回运行中的容器对象
     *
     * @return 容器对象，如果没有返回null
     */
    fun getRunningContainer() : Container? = TestContainerKit.getRunningContainer(LABEL)

    @JvmStatic
    fun main(args: Array<String>?) {
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "H2")
        startIfNeeded(null)
        println("H2 ${container.host} port: $PORT")
        Thread.sleep(Long.MAX_VALUE)
    }

}

package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * smtp测试容器
 *
 * @author K
 * @since 1.0.0
 */
object SmtpTestContainer {

    const val IMAGE_NAME = "namshi/smtp:latest"

    const val LABEL = "Smtp"

    private val container = GenericContainer(DockerImageName.parse(IMAGE_NAME)).apply {
        withExposedPorts(25)
        // Exim（namshi/smtp 里用的 MTA）默认不允许中继。这里开放中继给测试网络。
        // 也可以改用专为测试设计的“收信黑洞/抓信”容器，如：axllent/mailpit容器。
        withEnv("RELAY_NETWORKS", ":172.16.0.0/12:10.0.0.0/8:192.168.0.0/16") // 或 ":0.0.0.0/0"
        withEnv("DISABLE_IPV6", "1")
        withEnv("OTHER_HOSTNAMES", "test.local") // 可选：把本机当作 test.local 的最终收件域
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
        return TestContainerCrossProcessLock.run(SmtpTestContainer::class.java, "smtp") {
            val runningContainer = TestContainerKit.startContainerIfNeeded(LABEL, container)
            if (registry != null) {
                registerProperties(registry, runningContainer)
            }
            runningContainer
        }
    }

    /**
     * SMTP 容器目前不向 Spring 注册任何属性——保留方法只为和其他 TestContainer 形态对齐，
     * 让 [startIfNeeded] 调用模板保持统一。后续如要透传 host/port 给业务配置（如 spring.mail.host），
     * 在此处补 `registry.add(...)` 即可。
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
        ManualTestContainerMainSupport.removeExistingContainers(LABEL, "Smtp")
        startIfNeeded(null)
        println("smtp localhost port: ${container.firstMappedPort}")
        Thread.sleep(Long.MAX_VALUE)
    }

}

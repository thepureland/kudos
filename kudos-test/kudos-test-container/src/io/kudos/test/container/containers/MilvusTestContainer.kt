package io.kudos.test.container.containers

import io.kudos.base.io.PathKit
import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

/**
 * kafka测试容器
 *
 * @author ChatGpt
 * @author K
 * @since 1.0.0
 */
object MilvusTestContainer {

    private val log = LoggerFactory.getLogger(MilvusTestContainer::class.java)

    private val composeFile = File("${PathKit.getRuntimePath()}/docker-compose-milvus.yml")

    // 二選一：
    // ① 用容器化 compose（更穩，不吃你本機 docker compose 版本）
    private val compose = ComposeContainer(
        DockerImageName.parse("docker:25.0.5"),
        composeFile
    )
        // Milvus gRPC 19530
        .withExposedService(
            "standalone-1",
            19530,
            Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5))
        )
        // Milvus HTTP 9091（healthz）
        .withExposedService(
            "standalone-1",
            9091,
            Wait.forHttp("/healthz").forPort(9091).forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(5))
        )

    @Volatile private var started = false

    fun startIfNeeded(registry: DynamicPropertyRegistry?) {
        synchronized(this) {
            if (!started) {
                compose.start()
                started = true
                log.info("Milvus compose started.")
            }

            if (registry != null) {
                val host = compose.getServiceHost("standalone-1", 19530)
                val port = compose.getServicePort("standalone-1", 19530)

                // 你按自己項目裡 milvus 的配置 key 來註冊
                registry.add("milvus.host") { host }
                registry.add("milvus.port") { port }
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        startIfNeeded(null)
        val host = compose.getServiceHost("standalone-1", 19530)
        val port = compose.getServicePort("standalone-1", 19530)
        println("Milvus endpoint: $host:$port")
        Thread.sleep(Long.MAX_VALUE)
    }

}

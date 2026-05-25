package io.kudos.test.container.main

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import org.testcontainers.DockerClientFactory
import kotlin.system.measureTimeMillis

/**
 * 手动启动单个测试容器前的清理工具。
 *
 * 仅供 `XxxTestContainer.main` 这类人工预启动入口使用；
 * 一般测试启动路径必须保留 kudos TestContainer 的跨 JVM 复用语义，不要在测试中调用本类。
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object ManualTestContainerMainSupport {

    /**
     * 删除所有带指定 label 的容器（含已退出的），为手动启动入口提供干净状态。
     *
     * @param label 容器助记名（Docker label 值）
     * @param displayName 日志中展示的容器名
     */
    fun removeExistingContainers(label: String, displayName: String) {
        val dockerClient: DockerClient = DockerClientFactory.lazyClient()
        // 手动预启动入口要提供干净容器；测试本身仍透过同一 label 复用，避免破坏并行测试。
        val containers: List<Container> = dockerClient.listContainersCmd()
            .withShowAll(true)
            .withLabelFilter(mapOf(TestContainerKit.LABEL_KEY to label))
            .exec()
        if (containers.isEmpty()) return
        for (container in containers) {
            removeContainer(dockerClient, container, displayName)
        }
    }

    private fun removeContainer(dockerClient: DockerClient, container: Container, displayName: String) {
        val id = container.id
        val shortId = if (id == null || id.length <= 12) id else id.substring(0, 12)
        println(">>>>>>>>>>>>>>>>>>>> Removing existing $displayName container: $shortId")
        // remove --force --volumes 同时覆盖 running/exited 状态，避免旧数据卷影响下一次手动启动。
        val elapsed = measureTimeMillis {
            dockerClient.removeContainerCmd(id)
                .withForce(true)
                .withRemoveVolumes(true)
                .exec()
        }
        println("<<<<<<<<<<<<<<<<<<<< Existing $displayName container removed in $elapsed ms: $shortId")
    }
}

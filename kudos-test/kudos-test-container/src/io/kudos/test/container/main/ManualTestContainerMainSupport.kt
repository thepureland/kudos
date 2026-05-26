package io.kudos.test.container.main

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import org.testcontainers.DockerClientFactory
import kotlin.system.measureTimeMillis

/**
 * Cleanup utility used before manually starting a single test container.
 *
 * Intended only for manual prelaunch entry points such as `XxxTestContainer.main`.
 * Do not call this from tests: the normal test startup path must preserve kudos TestContainer's
 * cross-JVM reuse semantics.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object ManualTestContainerMainSupport {

    /**
     * Removes all containers (including exited ones) with the given label, providing a clean state for
     * the manual startup entry point.
     *
     * @param label container mnemonic name (Docker label value)
     * @param displayName container name shown in logs
     */
    fun removeExistingContainers(label: String, displayName: String) {
        val dockerClient: DockerClient = DockerClientFactory.lazyClient()
        // The manual prelaunch entry point must provide clean containers; tests themselves still reuse via the same label, so parallel tests are not disrupted.
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
        // remove --force --volumes covers both running/exited states, preventing stale data volumes from affecting the next manual startup.
        val elapsed = measureTimeMillis {
            dockerClient.removeContainerCmd(id)
                .withForce(true)
                .withRemoveVolumes(true)
                .exec()
        }
        println("<<<<<<<<<<<<<<<<<<<< Existing $displayName container removed in $elapsed ms: $shortId")
    }
}

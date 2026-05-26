package io.kudos.test.container.containers

import com.github.dockerjava.api.model.Container
import io.kudos.test.container.kit.TestContainerKit
import io.kudos.test.container.main.ManualTestContainerMainSupport
import io.kudos.test.container.support.TestContainerCrossProcessLock
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * smtp test container.
 *
 * @author K
 * @since 1.0.0
 */
object SmtpTestContainer {

    const val IMAGE_NAME = "namshi/smtp:latest"

    const val LABEL = "Smtp"

    private val container = GenericContainer(DockerImageName.parse(IMAGE_NAME)).apply {
        withExposedPorts(25)
        // Exim (the MTA used by namshi/smtp) does not allow relaying by default. Open relaying to the test networks here.
        // Alternatively, use a container designed for testing (e.g. a mail black-hole/capture container such as axllent/mailpit).
        withEnv("RELAY_NETWORKS", ":172.16.0.0/12:10.0.0.0/8:192.168.0.0/16") // or ":0.0.0.0/0"
        withEnv("DISABLE_IPV6", "1")
        withEnv("OTHER_HOSTNAMES", "test.local") // Optional: treat this host as the final destination domain for test.local
        withLabel(TestContainerKit.LABEL_KEY, LABEL)
    }

    /**
     * Starts the container (if needed).
     *
     * Ensures a single container is shared across a batch of tests, avoiding the time wasted starting/stopping containers repeatedly.
     * Alternatively, you can run this class's main method manually to start the container and share it while running tests.
     * Registers a JVM shutdown hook to automatically stop the container when the batch finishes,
     * rather than stopping after each test — provided the @Testcontainers annotation is not used.
     * To skip tests when Docker is not installed, use @EnabledIfDockerInstalled.
     *
     * @param registry Spring's dynamic property registry, used to register or override already-registered properties
     * @return the running container instance
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
     * The SMTP container currently registers no Spring properties — the method is kept to keep
     * the [startIfNeeded] call template aligned with other TestContainer types. If host/port need to be
     * passed into business config (e.g. spring.mail.host) later, add `registry.add(...)` calls here.
     *
     * @param registry the Spring dynamic property registry (null allowed)
     * @param runningContainer the running container
     * @author K
     * @since 1.0.0
     */
    private fun registerProperties(registry: DynamicPropertyRegistry?, runningContainer: Container) {
    }

    /**
     * Returns the running container instance.
     *
     * @return the container instance, or null if none is running
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

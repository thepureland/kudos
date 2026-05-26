package io.kudos.test.container.annotations

import org.junit.jupiter.api.extension.ExtendWith
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Enables a test case if Docker is installed on the local machine.
 *
 * Unlike `@EnabledIfDockerAvailable`, this annotation only checks whether Docker is installed
 * (via the `docker --version` command), not whether Docker is running. If Docker is installed
 * but not started, the test still runs (DockerKit will start Docker automatically).
 *
 * If Docker is not installed, the test case is skipped.
 * 
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
@Target(CLASS, FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(DockerInstalledCondition::class)
annotation class EnabledIfDockerInstalled

package io.kudos.test.container.annotations

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Condition executor that checks whether Docker is installed.
 *
 * Runs the `docker --version` command to determine whether Docker is installed.
 * A successful command means Docker is installed; otherwise it is not.
 * 
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
class DockerInstalledCondition : ExecutionCondition {
    
    companion object {
        @Volatile
        private var dockerInstalled: Boolean? = null
        private val lock = Any()

        /** Maximum time to wait for `docker --version` before treating Docker as not installed. */
        private const val CHECK_TIMEOUT_SECONDS = 10L
    }
    
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val installed = isDockerInstalled()
        
        return if (installed) {
            ConditionEvaluationResult.enabled("Docker is installed")
        } else {
            ConditionEvaluationResult.disabled("Docker is not installed; skipping test case")
        }
    }
    
    /**
     * Check whether Docker is installed.
     *
     * @return true if Docker is installed, false otherwise
     */
    private fun isDockerInstalled(): Boolean {
        // Use double-checked locking to avoid running the command multiple times
        if (dockerInstalled != null) {
            return requireNotNull(dockerInstalled)
        }
        
        synchronized(lock) {
            if (dockerInstalled != null) {
                return requireNotNull(dockerInstalled)
            }
            
            dockerInstalled = checkDockerInstalled()
            return requireNotNull(dockerInstalled)
        }
    }
    
    /**
     * Check whether Docker is installed by running the `docker --version` command.
     */
    private fun checkDockerInstalled(): Boolean {
        return try {
            val process = ProcessBuilder("docker", "--version")
                .redirectErrorStream(true)
                .start()

            // Bounded wait: a hung docker CLI must not block the whole test-discovery phase forever
            if (!process.waitFor(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return false
            }
            process.exitValue() == 0
        } catch (e: IOException) {
            // If the command fails (e.g. docker command not found), Docker is not installed
            false
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

}

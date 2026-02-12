package io.kudos.test.container.annotations

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.IOException

/**
 * 检查 Docker 是否已安装的条件执行器
 * 
 * 通过执行 `docker --version` 命令来判断 Docker 是否已安装。
 * 如果命令执行成功，说明 Docker 已安装；否则说明未安装。
 * 
 * @author K
 * @author AI:Cursor
 * @since 1.0.0
 */
class DockerInstalledCondition : ExecutionCondition {
    
    companion object {
        private var dockerInstalled: Boolean? = null
        private val lock = Any()
    }
    
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val installed = isDockerInstalled()
        
        return if (installed) {
            ConditionEvaluationResult.enabled("Docker 已安装")
        } else {
            ConditionEvaluationResult.disabled("Docker 未安装，跳过测试用例")
        }
    }
    
    /**
     * 检查 Docker 是否已安装
     * 
     * @return true 如果 Docker 已安装，false 如果未安装
     */
    private fun isDockerInstalled(): Boolean {
        // 使用双重检查锁定模式，避免多次执行命令
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
     * 通过执行 `docker --version` 命令检查 Docker 是否已安装
     */
    private fun checkDockerInstalled(): Boolean {
        return try {
            val process = ProcessBuilder("docker", "--version")
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: IOException) {
            // 如果命令执行失败（如找不到 docker 命令），说明 Docker 未安装
            false
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

}

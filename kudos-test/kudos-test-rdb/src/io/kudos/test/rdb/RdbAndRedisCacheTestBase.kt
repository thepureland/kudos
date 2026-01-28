package io.kudos.test.rdb

import io.kudos.test.container.containers.H2TestContainer
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * 所有需要关系型数据库和Redis缓存环境的测试用例的父类
 *
 * ## 背景与使用场景
 * - 位于测试层，继承自AbstractRdbTestBase，为需要完整数据库和Redis环境的测试提供容器配置
 * - 由Service测试类等需要完整环境的测试类继承使用
 * - 继承AbstractRdbTestBase的数据库容器配置功能（默认H2）
 *
 * ## 责任边界
 * - 启动并配置H2TestContainer和RedisTestContainer
 * - 配置缓存策略
 * - 继承AbstractRdbTestBase的所有功能（测试数据加载、串行执行、事务回滚）
 *
 * ## 核心流程
 * 1. 在@DynamicPropertySource中启动H2TestContainer和RedisTestContainer
 * 2. 配置缓存相关属性
 * 3. 继承AbstractRdbTestBase的测试数据加载和事务回滚功能
 *
 * ## 依赖与外部交互
 * - 依赖：AbstractRdbTestBase（提供测试数据加载、事务管理）
 * - 依赖：H2TestContainer, RedisTestContainer
 * - IO：启动Docker容器（如果未运行）
 *
 * ## 资料/契约
 * - 输入：无
 * - 输出：配置Spring测试环境属性（H2、Redis和缓存相关）
 * - 错误：如果Docker未安装或容器启动失败，会抛出异常
 *
 * ## 交易与一致性
 * - 事务：继承AbstractRdbTestBase的@Transactional，测试完成后自动回滚
 *
 * ## 并发与线程安全
 * - 容器启动使用同步机制，确保只启动一次
 * - 继承AbstractRdbTestBase的串行执行保证
 *
 * ## 性能特性
 * - 容器启动有一定开销，但会复用已运行的容器
 *
 * ## 安全与合规
 * - 仅用于测试环境，不涉及生产数据
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
open class RdbAndRedisCacheTestBase : SqlTestBase() {

    companion object Companion {

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            val ryukDisabledKey = "testcontainers.ryuk.disabled"
            val ryukDisabled = System.getProperty(ryukDisabledKey)
            if (ryukDisabled.isNullOrBlank()) {
                System.setProperty(ryukDisabledKey, "true")
                // #region agent log
                kotlin.runCatching {
                    val payload =
                        """{"sessionId":"debug-session","runId":"post-fix","hypothesisId":"H5","location":"RdbAndRedisCacheTestBase.registerProperties","message":"ryuk disabled via system property","data":{"key":"$ryukDisabledKey","value":"true"},"timestamp":${System.currentTimeMillis()}}"""
                    java.io.File("/Users/will/dev/code/kudos/.cursor/debug.log").appendText(payload + "\n")
                }
                // #endregion
            } else {
                // #region agent log
                kotlin.runCatching {
                    val payload =
                        """{"sessionId":"debug-session","runId":"post-fix","hypothesisId":"H5","location":"RdbAndRedisCacheTestBase.registerProperties","message":"ryuk already configured","data":{"key":"$ryukDisabledKey","value":"$ryukDisabled"},"timestamp":${System.currentTimeMillis()}}"""
                    java.io.File("/Users/will/dev/code/kudos/.cursor/debug.log").appendText(payload + "\n")
                }
                // #endregion
            }
            // #region agent log
            kotlin.runCatching {
                val payload =
                    """{"sessionId":"debug-session","runId":"pre-fix","hypothesisId":"H1","location":"RdbAndRedisCacheTestBase.registerProperties","message":"dynamic properties registration start","data":{"thread":"${Thread.currentThread().name}"},"timestamp":${System.currentTimeMillis()}}"""
                java.io.File("/Users/will/dev/code/kudos/.cursor/debug.log").appendText(payload + "\n")
            }
            // #endregion
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("cache.config.strategy") { "SINGLE_LOCAL" } //TODO 由子类指定

            val h2Thread = Thread { H2TestContainer.startIfNeeded(registry) } //TODO 由子类指定具体的TestContainer
            val redisThread = Thread { RedisTestContainer.startIfNeeded(registry) }

            h2Thread.start()
            redisThread.start()

            h2Thread.join()
            redisThread.join()
            // #region agent log
            kotlin.runCatching {
                val payload =
                    """{"sessionId":"debug-session","runId":"pre-fix","hypothesisId":"H1","location":"RdbAndRedisCacheTestBase.registerProperties","message":"dynamic properties registration done","data":{"h2ThreadAlive":${h2Thread.isAlive},"redisThreadAlive":${redisThread.isAlive}},"timestamp":${System.currentTimeMillis()}}"""
                java.io.File("/Users/will/dev/code/kudos/.cursor/debug.log").appendText(payload + "\n")
            }
            // #endregion
        }
    }

}
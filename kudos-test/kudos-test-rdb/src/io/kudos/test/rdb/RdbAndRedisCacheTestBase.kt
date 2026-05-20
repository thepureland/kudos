package io.kudos.test.rdb

import io.kudos.test.container.containers.H2TestContainer
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.concurrent.thread

/**
 * 所有需要关系型数据库和Redis缓存环境的测试用例的父类
 *
 * ## 背景与使用场景
 * - 位于测试层，继承自[SqlTestBase]，为需要完整数据库和Redis环境的测试提供容器配置
 * - 由Service测试类等需要完整环境的测试类继承使用
 * - 注意：本类不继承 [RdbTestBase]——两者对 `kudos.ability.cache.enabled` 配置互斥，无法叠加
 *
 * ## 责任边界
 * - 启动并配置H2TestContainer和RedisTestContainer
 * - 配置缓存策略
 * - 继承[SqlTestBase]的所有功能（测试数据加载、串行执行、事务回滚）
 * - 在 fixture 加载后通过 [CacheTestResetSupport] 重置 Redis + 应用缓存
 *
 * ## 核心流程
 * 1. 在@DynamicPropertySource中并行启动H2TestContainer和RedisTestContainer
 * 2. 配置缓存相关属性（启用缓存，默认 SINGLE_LOCAL 策略）
 * 3. 继承[SqlTestBase]的测试数据加载和事务回滚功能
 * 4. 每次 fixture 加载完成后 flush Redis + reload 应用缓存
 *
 * ## 依赖与外部交互
 * - 依赖：[SqlTestBase]（提供测试数据加载、事务管理）
 * - 依赖：H2TestContainer, RedisTestContainer
 * - IO：启动Docker容器（如果未运行）
 *
 * ## 资料/契约
 * - 输入：无
 * - 输出：配置Spring测试环境属性（H2、Redis和缓存相关）
 * - 错误：如果Docker未安装或容器启动失败，会抛出异常
 *
 * ## 交易与一致性
 * - 事务：继承[SqlTestBase]的@Transactional；fixture 数据由 @BeforeTransaction 在事务外提交
 *
 * ## 并发与线程安全
 * - 容器启动使用同步机制，确保只启动一次
 * - 继承[SqlTestBase]的串行执行保证
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

    override fun afterTestDataSetup() {
        CacheTestResetSupport.resetRedisAndApplicationCaches()
    }

    companion object Companion {

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            val ryukDisabledKey = "testcontainers.ryuk.disabled"
            if (System.getProperty(ryukDisabledKey).isNullOrBlank()) {
                System.setProperty(ryukDisabledKey, "true")
            }
            registry.add("kudos.ability.cache.enabled") { "true" }
            // 默认 SINGLE_LOCAL；要验 LOCAL_AND_REMOTE / SINGLE_REMOTE 时业务子类自己覆盖
            registry.add("cache.config.strategy") { "SINGLE_LOCAL" }

            val h2Thread = thread(name = "h2-testcontainer-start") { H2TestContainer.startIfNeeded(registry) }
            val redisThread = thread(name = "redis-testcontainer-start") { RedisTestContainer.startIfNeeded(registry) }

            h2Thread.join()
            redisThread.join()
        }
    }

}

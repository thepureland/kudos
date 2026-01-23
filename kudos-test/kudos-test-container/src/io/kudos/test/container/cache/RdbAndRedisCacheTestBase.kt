package io.kudos.test.container.cache

import io.kudos.base.support.Single
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.H2TestContainer
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional

/**
 * 所有需要关系型数据库和Redis缓存环境的测试用例的父类
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Transactional
open class RdbAndRedisCacheTestBase {

    companion object Companion {

        @JvmStatic
        protected val cacheStrategyHolder = Single("SINGLE_LOCAL")

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("cache.config.strategy") { cacheStrategyHolder.value }

            val h2Thread = Thread { H2TestContainer.startIfNeeded(registry) } //TODO 由子类指定具体的TestContainer
            val redisThread = Thread { RedisTestContainer.startIfNeeded(registry) }

            h2Thread.start()
            redisThread.start()

            h2Thread.join()
            redisThread.join()
        }
    }

}
package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.containers.H2TestContainer
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional


/**
 * 所有CacheHandlerTest的父类
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@Transactional
open class CacheHandlerTestBase {

    /**
     * 缓存策略，子类可以通过覆盖此方法来提供不同的缓存策略
     * 默认返回 SINGLE_LOCAL
     */
    open fun getCacheStrategy(): String = CacheStrategy.SINGLE_LOCAL.name

    companion object {

        /**
         * 获取缓存策略，通过反射查找实际测试类的 getCacheStrategy 方法
         * 如果子类覆盖了 getCacheStrategy 方法，则使用子类的实现
         * 否则使用默认的 SINGLE_LOCAL
         */
        @JvmStatic
        private fun resolveCacheStrategy(): String {
            // 通过堆栈跟踪找到实际调用 registerProperties 的测试类
            // 跳过前几个堆栈帧（当前方法、registerProperties、Spring框架等）
            val stackTrace = Thread.currentThread().stackTrace
            for (i in 3 until stackTrace.size) {
                val element = stackTrace[i]
                val className = element.className
                try {
                    val clazz = Class.forName(className)
                    // 检查是否是 CacheHandlerTestBase 的子类，且不是基类本身
                    if (CacheHandlerTestBase::class.java.isAssignableFrom(clazz) && 
                        clazz != CacheHandlerTestBase::class.java) {
                        // 尝试创建实例并调用 getCacheStrategy 方法
                        val instance = clazz.getDeclaredConstructor().newInstance() as CacheHandlerTestBase
                        return instance.getCacheStrategy()
                    }
                } catch (_: Exception) {
                    // 忽略无法加载或实例化的类，继续查找
                    continue
                }
            }
            // 如果找不到子类，返回默认值
            return CacheStrategy.SINGLE_LOCAL.name
        }

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("cache.config.strategy") { resolveCacheStrategy() }

            val h2Thread = Thread { H2TestContainer.startIfNeeded(registry) }
            val redisThread = Thread { RedisTestContainer.startIfNeeded(registry) }

            h2Thread.start()
            redisThread.start()

            h2Thread.join()
            redisThread.join()
        }
    }

}

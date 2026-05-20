package io.kudos.ability.cache.remote.redis

import io.kudos.context.kit.SpringKit
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.core.RedisTemplate

/**
 * [RedisCache] 的 fix 子类：override [clear] 用 `RedisTemplate.keys(pattern) + delete(keys)` 直删，
 * 绕开 Spring Boot 4.0.6 自带 `RedisCache.clear()` 的 bug。
 *
 * ## 背景
 *
 * Spring Boot 4.0.6 / Spring Data Redis 4.x 自带的 [RedisCache.clear] 调用最终走
 * `cacheWriter.clean(name, pattern)`，但实测在 kudos 的 Redis 配置下**永远不删除任何 key**——
 * SCAN+DEL 一遍下来 Redis 里依旧有所有匹配的 key（用 [RedisTemplate.keys] 直查可以确认）。
 * 推测是 Spring 内部 `pattern` bytes 与实际存储的 key bytes 不匹配（可能是不同的序列化路径），
 * 但社区上目前没有 fix。
 *
 * 这个 bug 直接导致 `DomainByNameCache.reloadAll(clear = true)` 之类的 "wipe and refill" 操作
 * 调完之后 Redis 里旧值还在，下次 `mixGet` 走 local-miss → remote-hit → 回填 local，缓存又脏了。
 * 一连串集成测试（SysDomainServiceTest / SysLocaleServiceTest / SysOutLineServiceTest 等）
 * 在 batchDelete + reloadAll 之后断言缓存 miss 时随机失败，根因都在这里。
 *
 * ## 修复
 *
 * Override [clear] 改成：
 * 1. 用 `RedisTemplate.keys("$keyPrefix*")` 列出 Redis 里所有匹配本缓存名前缀的 key。
 * 2. 用 `RedisTemplate.delete(keys)` 一次性删除。
 *
 * 这是 Spring 文档建议的"知道哪些 key 就直接 delete"的标准用法，行为可预测，跟测试基础设施已经
 * 使用的 `RedisTemplate` 走同一条路径。`RedisTemplate` 通过 [SpringKit] 懒查（每次 clear 都查一遍，
 * 开销可忽略——clear 是低频操作）。
 *
 * ## 限制
 *
 * [RedisTemplate.keys] 在大库上会阻塞 Redis；本项目缓存键数量级很小（百级），可接受。如果未来缓存
 * 体量大了，可以改用 `SCAN`-based 迭代删除（见 [RedisKeyValueCacheManager.evictByPattern] 的写法）。
 *
 * @author K
 * @since 1.0.0
 */
internal class ScanClearRedisCache(
    name: String,
    cacheWriter: RedisCacheWriter,
    cacheConfiguration: RedisCacheConfiguration,
) : RedisCache(name, cacheWriter, cacheConfiguration) {

    override fun clear() {
        val template = redisTemplate() ?: run {
            // 容器里没有 RedisTemplate（极少见，比如纯单元测试 mock）—— fallback 到 Spring 默认逻辑
            super.clear()
            return
        }
        val pattern = "${cacheConfiguration.getKeyPrefixFor(name)}*"
        val matched = template.keys(pattern)
        if (matched.isNotEmpty()) {
            template.delete(matched)
        }
    }

    private fun redisTemplate(): RedisTemplate<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        return SpringKit.getBeansOfType<RedisTemplate<*, *>>()
            .let { it["stringRedisTemplate"] ?: it.values.firstOrNull() }
            as? RedisTemplate<String, Any>
    }
}

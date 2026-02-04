package io.kudos.ability.cache.local.caffeine.keyvalue

import io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable
import io.kudos.base.logger.LogFactory
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 批量缓存测试模拟服务
 *
 * @author K
 * @since 1.0.0
 */
@CacheConfig(cacheNames = ["test"])
open class BatchCacheableTestService {

    private val log = LogFactory.getLog(this)

    private var allData = listOf(
        TestCacheObject("1", 2, "5", null, 7),
        TestCacheObject("1", 3, "6", null, 7),
        TestCacheObject("1", 4, "5", null, 7),
        TestCacheObject("1", 2, "6", null, 7),
        TestCacheObject("1", 3, "5", null, 7),
        TestCacheObject("1", 4, "6", null, 7)
    )

    @Cacheable(key = "#module.concat('::').concat(#age).concat('::').concat(#name).concat('::').concat(#type)")
    open fun load(module: String, age: Int, name: String, active: Boolean, type: Int): List<TestCacheObject> {
        log.debug("单条加载数据，参数：$module, $age, $name, $type")
        Thread.sleep(1000) // 模拟耗时的io操作
        val data = allData.filter { it.module == module && it.age == age && it.name == name && it.type == type }
        val result = mutableListOf<TestCacheObject>()
        val now = LocalDateTime.now()
        data.forEach {
            val another = it.copy()
            another.time = now
            result.add(another)
        }
        return result
    }

    @BatchCacheable(valueClass = List::class, ignoreParamIndexes = [3])
    open fun batchLoad(
        module: String, ages: List<Int>, names: Array<String>, active: Boolean, type: Int
    ): Map<String, List<TestCacheObject>> {
        log.debug("批量加载数据，参数：$module, $ages, ${names.toList()}, $type")
        Thread.sleep(1000) // 模拟耗时的io操作
        val list = allData.filter { it.module == module && it.age in ages && it.name in names && it.type == type }
        val result = linkedMapOf<String, List<TestCacheObject>>()
        val now = LocalDateTime.now()
        list.forEach {
            val another = it.copy()
            another.time = now
            result["${it.module}::${it.age}::${it.name}::${it.type}"] = listOf(another)
        }
        return result
    }

    data class TestCacheObject(
        val module: String,
        val age: Int,
        val name: String,
        var time: LocalDateTime?,
        val type: Int
    ) : Serializable

}


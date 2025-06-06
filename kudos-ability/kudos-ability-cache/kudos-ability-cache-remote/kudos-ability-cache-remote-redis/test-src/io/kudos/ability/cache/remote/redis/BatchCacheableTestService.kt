package io.kudos.ability.cache.remote.redis

import io.kudos.base.logger.LogFactory
import org.soul.ability.cache.common.batch.BatchCacheable
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import java.io.Serializable
import java.time.LocalDateTime

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

//    @XmlRootElement
    data class TestCacheObject(
        val module: String?,
        val age: Int?,
        val name: String?,
//        @get:JsonSerialize(using = LocalDateTimeSerializer::class)
//        @set:JsonDeserialize(using = LocalDateTimeDeserializer::class)
        var time: LocalDateTime?,
        val type: Int?
    ) : Serializable {

        constructor(): this(null, null, null, null, null)

    }

}


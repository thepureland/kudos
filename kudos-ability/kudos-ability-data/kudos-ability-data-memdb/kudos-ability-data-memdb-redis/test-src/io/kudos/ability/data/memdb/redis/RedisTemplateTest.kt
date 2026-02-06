package io.kudos.ability.data.memdb.redis

import io.kudos.base.time.toLocalDateTime
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * redis测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
internal class RedisTemplateTest {

    @Resource
    private lateinit var redisTemplate: RedisTemplate<Any, Any?>

    @Test
    fun test() {
        val pair = Pair("1st", "2nd")
        redisTemplate.opsForValue().set("test", pair)
        assertEquals(redisTemplate.opsForValue().get("test"), pair)

        val obj = TestObject("module", 18, "name", Date().toLocalDateTime())
        redisTemplate.opsForValue().set("obj", obj)
        assertEquals(redisTemplate.opsForValue().get("obj"), obj)

        // 只有jdk序列化方式才可以
//        val map = mapOf(obj to "value")
//        redisTemplate.opsForValue().set("map", map)
//        assertTrue((redisTemplate.opsForValue().get("map") as Map<*, *>)[obj] == "value")
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
        }
    }

}

data class TestObject(
    val module: String?,
    val age: Int?,
    val name: String?,
//        @get:JsonSerialize(using = LocalDateTimeSerializer::class)
//        @set:JsonDeserialize(using = LocalDateTimeDeserializer::class)
    var time: LocalDateTime?,
) : Serializable {

    constructor(): this(null, null, null, null)

}
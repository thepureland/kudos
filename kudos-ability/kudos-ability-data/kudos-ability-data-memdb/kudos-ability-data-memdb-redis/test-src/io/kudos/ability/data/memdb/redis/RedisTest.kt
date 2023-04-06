package io.kudos.ability.data.memdb.redis

import io.kudos.base.time.toLocalDateTime
import io.kudos.test.common.SpringTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

/**
 * redis测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class RedisTest : SpringTest() {

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<Any, Any?>

    @Test
    fun test() {
        val pair = Pair("1st", "2nd")
        redisTemplate.opsForValue().set("test", pair)
        assertTrue(redisTemplate.opsForValue().get("test") == pair)

        val obj = TestObject("module", 18, "name", Date().toLocalDateTime())
        redisTemplate.opsForValue().set("obj", obj)
        assertTrue(redisTemplate.opsForValue().get("obj") == obj)

        // 只有jdk序列化方式才可以
//        val map = mapOf(obj to "value")
//        redisTemplate.opsForValue().set("map", map)
//        assertTrue((redisTemplate.opsForValue().get("map") as Map<*, *>)[obj] == "value")
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
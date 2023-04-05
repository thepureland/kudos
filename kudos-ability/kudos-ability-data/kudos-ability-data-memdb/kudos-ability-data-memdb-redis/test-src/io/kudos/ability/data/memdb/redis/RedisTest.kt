package io.kudos.ability.data.memdb.redis

import io.kudos.test.common.SpringTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

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
    }

}
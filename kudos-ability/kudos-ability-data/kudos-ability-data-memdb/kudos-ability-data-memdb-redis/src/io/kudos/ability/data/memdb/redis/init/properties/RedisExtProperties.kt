package io.kudos.ability.data.memdb.redis.init.properties

import io.kudos.ability.data.memdb.redis.RedisSerializerEnum
import org.springframework.beans.BeanUtils
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * 单个 Redis 实例的扩展配置。继承 Spring Boot 的 [DataRedisProperties]（自带 host / port
 * / database / password / cluster / ssl 等），并叠加：
 *  - 4 个序列化器选择（[keySerializer] / [hashkeySerializer] / [valueSerializer] / [hashvalueSerializer]）
 *  - Apache commons-pool2 连接池参数（[maxIdle] / [minIdle] / [maxActive] / [maxWait]）
 *
 * 序列化器字段取值需匹配 [RedisSerializerEnum.type] 字面值；未匹配会启动时抛错。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisExtProperties : DataRedisProperties() {
    /** key 序列化器类型；缺省 [RedisSerializerEnum.STRING]。 */
    var keySerializer: String? = RedisSerializerEnum.STRING.type

    /** hash field 序列化器类型；缺省 [RedisSerializerEnum.JDK]。 */
    var hashkeySerializer: String? = RedisSerializerEnum.JDK.type

    /** value 序列化器类型；缺省 [RedisSerializerEnum.JDK]。 */
    var valueSerializer: String? = RedisSerializerEnum.JDK.type

    /** hash value 序列化器类型；缺省 [RedisSerializerEnum.JDK]。 */
    var hashvalueSerializer: String? = RedisSerializerEnum.JDK.type

    /** 连接池中最大空闲连接数。 */
    var maxIdle: Int = 8

    /**
     * Target for the minimum number of idle connections to maintain in the pool. This
     * setting only has an effect if both it and time between eviction runs are
     * positive.
     */
    var minIdle: Int = 0

    /**
     * Maximum number of connections that can be allocated by the pool at a given
     * time. Use a negative value for no limit.
     */
    var maxActive: Int = 8

    /**
     * Maximum amount of time a connection allocation should block before throwing an
     * exception when the pool is exhausted. Use a negative value to block
     * indefinitely.
     */
    var maxWait: Duration? = Duration.ofMillis(-1)

    /** 解析 [valueSerializer] 配置为可直接装到 RedisTemplate 上的实例。 */
    fun valueSerializer(): RedisSerializer<*> {
        return getSerializerByType(this.valueSerializer)
    }

    /** 解析 [hashvalueSerializer]。 */
    fun hashvalueSerializer(): RedisSerializer<*> {
        return getSerializerByType(this.hashvalueSerializer)
    }

    /** 解析 [keySerializer]。 */
    fun keySerializer(): RedisSerializer<*> {
        return getSerializerByType(this.keySerializer)
    }

    /** 解析 [hashkeySerializer]。 */
    fun hashkeySerializer(): RedisSerializer<*> {
        return getSerializerByType(this.hashkeySerializer)
    }

    /**
     * 按字面 [type] 实例化对应的序列化器。
     * 特殊处理：[StringRedisSerializer] 不走 `instantiateClass`，复用单例 `UTF_8` 实例。
     * 不识别的 type 立刻抛错（启动失败优于运行期意外）。
     */
    private fun getSerializerByType(type: String?): RedisSerializer<*> {
        val redisSerializerEnum = RedisSerializerEnum.ofEnum(type)
            ?: throw RuntimeException("指定的redisSerializer不存在于RedisSerializerEnum枚举中！")
        val serializerClazz = redisSerializerEnum.serializerClazz
        if (serializerClazz.isAssignableFrom(StringRedisSerializer::class.java)) {
            return StringRedisSerializer.UTF_8
        }
        return BeanUtils.instantiateClass(serializerClazz)
    }
}

package io.kudos.ability.data.memdb.redis

import com.alibaba.fastjson2.support.spring.data.redis.GenericFastJsonRedisSerializer
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer


/**
 * Redis 序列化字典表。配置 `kudos.ability.data.redis.redis-map.<name>.key-serializer` 等
 * 字段时填写本枚举的 [type] 字面值，运行时由 [RedisExtProperties.getSerializerByType]
 * 反射构造对应的 [RedisSerializer]。
 *
 * 已支持：
 *  - [STRING] —— UTF-8 字符串序列化
 *  - [JDK] —— Java 原生 `Serializable`；二进制紧凑但跨服务版本耦合强
 *  - [FASTJSON] —— Fastjson2 JSON 序列化；可读性高、跨语言友好
 *
 * 新增序列化器需在此处加枚举值 + 给 `getSerializerByType` 加分支。
 *
 * @author K
 * @since 1.0.0
 */
enum class RedisSerializerEnum(
    /** yml 配置中的字面值标识。 */
    val type: String,
    /** 对应的 Spring Data Redis 序列化器类型。 */
    val serializerClazz: Class<out RedisSerializer<*>>
) {

    /** UTF-8 字符串序列化器。 */
    STRING("string", StringRedisSerializer::class.java),

    /** JDK 序列化器（要求对象实现 `Serializable`）。 */
    JDK("jdk", JdkSerializationRedisSerializer::class.java),

    /** Fastjson2 JSON 序列化器；建议跨服务共享数据时使用。 */
    FASTJSON("fastjson", GenericFastJsonRedisSerializer::class.java);

    companion object {
        /**
         * 按 [type] 字面值查询枚举；未匹配返回 null。
         */
        fun ofEnum(type: String?): RedisSerializerEnum? = entries.find { it.type == type }
    }
}

package io.kudos.ability.cache.common.aop.hash

import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass

/**
 * 基于 Hash 结构的缓存注解，语义类似 Spring 的 [org.springframework.cache.annotation.Cacheable]。
 *
 * 仅支持**按 id 查单条**：方法返回值需为 [IIdEntity] 或其子类（可空）；key 为 SpEL，解析结果为该实体的 id。
 * - 命中：从 Hash 缓存 getById 直接返回
 * - 未命中：执行方法，若返回值非空则 save 入 Hash 缓存后返回
 *
 * 使用前需在缓存配置中为该 [cacheNames] 配置 `hash = true`。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class HashCacheable(

    /** Hash 缓存名称（需在配置中存在且 hash=true） */
    val cacheNames: Array<String> = [],

    /**
     * 缓存 key 的 SpEL 表达式，解析结果作为实体 id 用于 getById/save。
     * 例如 "#id"、 "#resourceId"。
     */
    val key: String = "#id",

    /**
     * 是否走缓存的 SpEL 条件，为空或解析为 true 时才查/写缓存。
     */
    val condition: String = "",

    /**
     * 是否不缓存的 SpEL 条件，解析为 true 时方法结果不写入缓存。
     */
    val unless: String = "",

    /** 缓存实体类型，用于 getById/save 的 KClass。 */
    val entityClass: KClass<out IIdEntity<*>>
)

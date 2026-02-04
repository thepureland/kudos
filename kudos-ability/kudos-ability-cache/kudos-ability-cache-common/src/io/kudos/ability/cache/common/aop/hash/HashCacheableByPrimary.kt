package io.kudos.ability.cache.common.aop.hash

import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass

/**
 * 按主属性可缓存的 Hash 注解，语义类似 Spring 的 [org.springframework.cache.annotation.Cacheable]。
 *
 * 术语：**主属性**即实体唯一标识（id）；**副属性**为除 id 外参与二级索引、列表查询与排序的属性。
 * 本注解仅支持**按主属性查单条**：方法返回值需为 [IIdEntity] 或其子类（可空）；[key] 为 SpEL，解析结果作为主属性（id）。
 * - 命中：从 Hash 缓存 getById 直接返回
 * - 未命中：执行方法，若返回值非空则 save 入 Hash 缓存后返回（可指定副属性以建 filterable/sortable 索引）
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
annotation class HashCacheableByPrimary(

    /** Hash 缓存名称（需在配置中存在且 hash=true） */
    val cacheNames: Array<String> = [],

    /**
     * 主属性的 SpEL 表达式，解析结果作为实体 id 用于 getById/save。
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
    val entityClass: KClass<out IIdEntity<*>>,

    /**
     * 可筛选副属性名（等值查询用 Set 索引）；为空则不建。例外：数值型的范围查询条件要放 [sortableProperties]。
     */
    val filterableProperties: Array<String> = [],

    /**
     * 可排序/范围副属性名（ZSet 索引）；用于 listPageByZSetIndex 等。例外：数值型范围查询条件放本项。
     */
    val sortableProperties: Array<String> = []
)

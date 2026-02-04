package io.kudos.ability.cache.common.aop.hash

import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass

/**
 * 按副属性等值查询的缓存注解：先按 [property]+[key] 查 Hash 缓存的 listBySetIndex，未命中则执行方法并将结果 saveBatch 回写。
 *
 * 术语：**主属性**为 id；**副属性**为参与索引与列表查询的属性（如 type、status）。本注解按副属性做等值列表查询。
 * 与具体实现无关，适用于 Caffeine、Redis 等任意 [IIdEntitiesHashCache] 实现。
 * 方法返回值需为 List&lt;IIdEntity&gt;；[property] 为副属性名，[key] 为 SpEL 解析出的副属性值。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class HashCacheableBySecondary(

    /** Hash 缓存名称（需在配置中存在且 hash=true） */
    val cacheNames: Array<String> = [],

    /** 副属性名（如 "type"），与 [key] 解析值组成 listBySetIndex(property, value)。 */
    val property: String = "type",

    /**
     * 副属性值的 SpEL 表达式，解析结果作为 listBySetIndex 的 value。
     * 例如 "#type"、 "#status"。
     */
    val key: String = "#type",

    /** 是否走缓存的 SpEL 条件，为空或解析为 true 时才查/写缓存。 */
    val condition: String = "",

    /** 是否不缓存的 SpEL 条件，解析为 true 时方法结果不写入缓存。 */
    val unless: String = "",

    /** 缓存实体类型，用于 listBySetIndex / saveBatch 的 KClass。 */
    val entityClass: KClass<out IIdEntity<*>>,

    /**
     * 可筛选副属性名（等值查询用 Set 索引），回写时建索引，需包含 [property]。例外：数值型范围查询条件放 [sortableProperties]。
     */
    val filterableProperties: Array<String> = [],

    /** 可排序/范围副属性名（ZSet 索引），回写时可选；数值型范围查询条件放本项。 */
    val sortableProperties: Array<String> = []
)

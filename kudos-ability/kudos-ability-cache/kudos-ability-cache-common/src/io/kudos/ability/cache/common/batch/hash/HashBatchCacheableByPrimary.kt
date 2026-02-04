package io.kudos.ability.cache.common.batch.hash

import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass

/**
 * 基于 Hash 的按主属性批量可缓存注解，语义参考 [BatchCacheable]：按一批主属性（id）先查 Hash 缓存，未命中的再调方法并回写。
 *
 * 术语：**主属性**即实体唯一标识（id）；**副属性**为除 id 外参与二级索引、列表查询与排序的属性。
 * 约束：
 * - 缓存 key 为主属性 id（String 或可转为 String），由 [keysGenerator] 从方法参数中生成 id 列表。
 * - 方法返回值必须为 [Map]，key 为 id，value 为 [IIdEntity]（可空）。
 * - 方法需为 open。
 * - 使用前需在缓存配置中为该 [cacheNames] 配置 `hash = true` 且 `writeInTime = true` 以便回写。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class HashBatchCacheableByPrimary(

    /** Hash 缓存名称（需在配置中存在且 hash=true） */
    val cacheNames: Array<String> = [],

    /** 实现 [io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator] 的 Spring Bean 名称，用于从参数生成主属性（id）列表 */
    val keysGenerator: String = "defaultHashBatchKeysGenerator",

    /** 缓存实体类型，用于 findByIds / saveBatch 的 KClass */
    val entityClass: KClass<out IIdEntity<*>>,

    /** 生成 key 时忽略的参数索引 */
    val ignoreParamIndexes: IntArray = [],

    /**
     * 可筛选副属性名（等值查询用 Set 索引）；为空则不建。例外：数值型范围查询条件要放 [sortableProperties]。
     */
    val filterableProperties: Array<String> = [],

    /**
     * 可排序/范围副属性名（ZSet 索引）；用于 listPageByZSetIndex 等。例外：数值型范围查询条件放本项。
     */
    val sortableProperties: Array<String> = []
)

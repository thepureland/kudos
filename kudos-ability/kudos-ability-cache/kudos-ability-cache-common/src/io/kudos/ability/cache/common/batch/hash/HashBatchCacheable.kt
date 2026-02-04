package io.kudos.ability.cache.common.batch.hash

import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass

/**
 * 基于 Hash 的批量可缓存注解，语义参考 [BatchCacheable]：按一批 id 先查 Hash 缓存，未命中的再调方法并回写。
 *
 * 约束：
 * - 缓存 key 为 id（String 或可转为 String），由 [keysGenerator] 从方法参数中生成 id 列表。
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
annotation class HashBatchCacheable(

    /** Hash 缓存名称（需在配置中存在且 hash=true） */
    val cacheNames: Array<String> = [],

    /** 实现 [io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator] 的 Spring Bean 名称，用于从参数生成 id 列表 */
    val keysGenerator: String = "defaultHashBatchKeysGenerator",

    /** 缓存实体类型，用于 findByIds / saveBatch 的 KClass */
    val entityClass: KClass<out IIdEntity<*>>,

    /** 生成 key 时忽略的参数索引 */
    val ignoreParamIndexes: IntArray = []
)

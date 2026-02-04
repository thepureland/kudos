package io.kudos.ability.cache.common.support

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass

/**
 * 基于 Hash 的“带 id 对象集合”缓存接口（同一抽象，本地/远程均由策略封装层委托）。
 *
 * 数据不必来自数据库表，只要是 [IIdEntity] 即可；支持按 id 存取、二级索引（Set/ZSet）查询、条件分页排序、全量刷新。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IIdEntitiesHashCache {

    fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E?

    fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        setIndexPropertyNames: Set<String> = emptySet(),
        zsetIndexPropertyNames: Set<String> = emptySet()
    )

    fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        setIndexPropertyNames: Set<String> = emptySet(),
        zsetIndexPropertyNames: Set<String> = emptySet()
    )

    fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        setIndexPropertyNames: Set<String> = emptySet(),
        zsetIndexPropertyNames: Set<String> = emptySet()
    )

    fun <E : IIdEntity<*>> findByIds(
        cacheName: String,
        ids: Collection<*>,
        entityClass: KClass<E>
    ): List<E>

    fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E>

    fun <PK, E : IIdEntity<PK>> listBySetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        property: String,
        value: Any
    ): List<E>

    fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        zsetIndexName: String,
        offset: Long,
        limit: Long,
        desc: Boolean = true
    ): List<E>

    fun <PK, E : IIdEntity<PK>> list(
        cacheName: String,
        entityClass: KClass<E>,
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<E>

    fun <PK, E : IIdEntity<PK>> refreshAll(
        cacheName: String,
        entities: List<E>,
        setIndexPropertyNames: Set<String> = emptySet(),
        zsetIndexPropertyNames: Set<String> = emptySet()
    )
}

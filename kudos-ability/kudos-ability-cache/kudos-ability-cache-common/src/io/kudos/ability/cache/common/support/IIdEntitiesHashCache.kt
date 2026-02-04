package io.kudos.ability.cache.common.support

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass

/**
 * 基于 Hash 的“带 id 对象集合”缓存接口（同一抽象，本地/远程均由策略封装层委托）。
 *
 * 术语：**主属性**即实体唯一标识（id），用于 getById/save/deleteById；**副属性**为除 id 外参与二级索引、列表查询与排序的属性（如 type、status、sortScore）。
 * [filterableProperties] 为等值筛选用 Set 索引；[sortableProperties] 为排序/范围用 ZSet 索引。例外：数值型的范围查询条件要放 sortableProperties。
 * 数据不必来自数据库表，只要是 [IIdEntity] 即可；支持按主属性存取、按副属性建索引并查询、条件分页排序、全量刷新。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IIdEntitiesHashCache {

    fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E?

    /**
     * 保存实体；[filterableProperties]/[sortableProperties] 为副属性名集合，用于构建 Set/ZSet 二级索引。数值型范围查询条件放 sortableProperties。
     */
    fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    )

    /**
     * 批量保存；[filterableProperties]/[sortableProperties] 为副属性名集合，用于构建 Set/ZSet 二级索引。数值型范围查询条件放 sortableProperties。
     */
    fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    )

    /**
     * 按主属性 id 删除；[filterableProperties]/[sortableProperties] 需与写入时一致，以便从副属性索引中移除。
     */
    fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    )

    fun <E : IIdEntity<*>> findByIds(
        cacheName: String,
        ids: Collection<*>,
        entityClass: KClass<E>
    ): List<E>

    fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E>

    /** 按副属性等值查询（Set 索引）：[property] 为副属性名，[value] 为属性值。 */
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

    /**
     * 全量刷新；[filterableProperties]/[sortableProperties] 为副属性名集合，用于重建 Set/ZSet 索引。
     */
    fun <PK, E : IIdEntity<PK>> refreshAll(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    )
}

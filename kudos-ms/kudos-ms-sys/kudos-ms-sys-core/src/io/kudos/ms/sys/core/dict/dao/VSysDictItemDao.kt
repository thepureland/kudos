package io.kudos.ms.sys.core.dict.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.model.po.VSysDictItem
import io.kudos.ms.sys.core.dict.model.table.VSysDictItems
import org.springframework.stereotype.Repository

/**
 * Read-only data access object for the dictionary item view (v_sys_dict_item).
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class VSysDictItemDao : BaseReadOnlyDao<String, VSysDictItem, VSysDictItems>() {

    /**
     * Query dictionary item (at most one) by atomic service code, dictionary type, item code and active flag.
     *
     * @param atomicServiceCode atomic service code
     * @param dictType dictionary type
     * @param itemCode dictionary item code
     * @return SysDictItemCacheEntry, or null when not found
     */
    open fun fetchByAtomicServiceCodeAndDictTypeAndItemCode(
        atomicServiceCode: String,
        dictType: String,
        itemCode: String
    ): SysDictItemCacheEntry? {
        val criteria = Criteria.and(
            VSysDictItem::atomicServiceCode eq atomicServiceCode,
            VSysDictItem::dictType eq dictType,
            VSysDictItem::itemCode eq itemCode,
            VSysDictItem::active eq true
        )
        return searchAs<SysDictItemCacheEntry>(criteria).firstOrNull()
    }

    /**
     * Query dictionary item list by atomic service code, dictionary type and active flag.
     *
     * @param atomicServiceCode atomic service code
     * @param dictType dictionary type
     * @return list of matching SysDictItemCacheEntry
     */
    open fun searchByAtomicServiceCodeAndDictType(
        atomicServiceCode: String,
        dictType: String
    ): List<SysDictItemCacheEntry> {
        val criteria = Criteria.and(
            VSysDictItem::atomicServiceCode eq atomicServiceCode,
            VSysDictItem::dictType eq dictType,
            VSysDictItem::active eq true
        )
        return searchAs<SysDictItemCacheEntry>(criteria, Order.asc("orderNum"))
    }

    /**
     * Query child dictionary item list by parent dictionary item id and active flag.
     *
     * @param parentId parent dictionary item id, non-null
     * @return list of matching SysDictItemCacheEntry, sorted by orderNum
     */
    open fun searchByParentId(parentId: String): List<SysDictItemCacheEntry> {
        val criteria = Criteria.and(
            VSysDictItem::parentId eq parentId,
            VSysDictItem::active eq true
        )
        return searchAs<SysDictItemCacheEntry>(criteria, Order.asc("orderNum"))
    }
}

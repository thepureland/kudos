package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.model.po.VSysDictItem
import io.kudos.ms.sys.core.model.table.VSysDictItems
import org.springframework.stereotype.Repository

/**
 * 字典项视图（v_sys_dict_item）数据访问对象，只读。
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class VSysDictItemDao : BaseReadOnlyDao<String, VSysDictItem, VSysDictItems>() {

    /**
     * 按原子服务编码、字典类型、字典项代码及启用状态查询字典项（至多一条）。
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @param itemCode 字典项代码
     * @return SysDictItemCacheEntry，不存在返回 null
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
     * 按原子服务编码、字典类型及启用状态查询字典项列表。
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return 匹配的 SysDictItemCacheEntry 列表
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
     * 按父字典项 id 及启用状态查询子字典项列表。
     *
     * @param parentId 父字典项 id，非空
     * @return 匹配的 SysDictItemCacheEntry 列表，按 orderNum 排序
     */
    open fun searchByParentId(parentId: String): List<SysDictItemCacheEntry> {
        val criteria = Criteria.and(
            VSysDictItem::parentId eq parentId,
            VSysDictItem::active eq true
        )
        return searchAs<SysDictItemCacheEntry>(criteria, Order.asc("orderNum"))
    }
}

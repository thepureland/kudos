package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.dict.SysDictCacheEntry
import io.kudos.ms.sys.core.model.po.SysDict
import io.kudos.ms.sys.core.model.table.SysDictItems
import io.kudos.ms.sys.core.model.table.SysDicts
import org.ktorm.dsl.eq
import org.springframework.stereotype.Repository


/**
 * 字典数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysDictDao : BaseCrudDao<String, SysDict, SysDicts>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 按原子服务编码及启用状态查询字典列表。
     *
     * @param atomicServiceCode 原子服务编码
     * @return 匹配的 SysDictCacheEntry 列表
     */
    open fun searchDictsByAtomicServiceCode(atomicServiceCode: String): List<SysDictCacheEntry> {
        val criteria = Criteria.and(
            SysDict::atomicServiceCode eq atomicServiceCode,
            SysDict::active eq true
        )
        return searchAs<SysDictCacheEntry>(criteria)
    }

    /**
     * 按原子服务编码、字典类型及启用状态查询字典（至多一条）。
     *
     * @param atomicServiceCode 原子服务编码
     * @param dictType 字典类型
     * @return SysDictCacheEntry，不存在返回 null
     */
    open fun fetchDictByAtomicServiceCodeAndDictType(atomicServiceCode: String, dictType: String): SysDictCacheEntry? {
        val criteria = Criteria.and(
            SysDict::atomicServiceCode eq atomicServiceCode,
            SysDict::dictType eq dictType,
            SysDict::active eq true
        )
        return searchAs<SysDictCacheEntry>(criteria).firstOrNull()
    }

    /**
     * 按字典ID删除对应的字典项记录。
     */
    open fun deleteDictItemsByDictId(dictId: String): Int {
        return batchDeleteWhen { column, _ ->
            if (column.name == SysDictItems.dictId.name) {
                column.eq(dictId)
            } else {
                null
            }
        }
    }

    //endregion your codes 2

}
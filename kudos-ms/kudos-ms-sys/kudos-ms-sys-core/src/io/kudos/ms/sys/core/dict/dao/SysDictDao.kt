package io.kudos.ms.sys.core.dict.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.core.dict.model.po.SysDict
import io.kudos.ms.sys.core.dict.model.table.SysDictItems
import io.kudos.ms.sys.core.dict.model.table.SysDicts
import org.ktorm.dsl.eq
import org.springframework.stereotype.Repository


/**
 * Dictionary data access object.
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysDictDao : BaseCrudDao<String, SysDict, SysDicts>() {


    /**
     * Query dictionary list by atomic service code and active flag.
     *
     * @param atomicServiceCode atomic service code
     * @return list of matching SysDictCacheEntry
     */
    open fun searchDictsByAtomicServiceCode(atomicServiceCode: String): List<SysDictCacheEntry> {
        val criteria = Criteria.and(
            SysDict::atomicServiceCode eq atomicServiceCode,
            SysDict::active eq true
        )
        return searchAs<SysDictCacheEntry>(criteria)
    }

    /**
     * Query dictionary (at most one) by atomic service code, dictionary type and active flag.
     *
     * @param atomicServiceCode atomic service code
     * @param dictType dictionary type
     * @return SysDictCacheEntry, or null when not found
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
     * Delete the dictionary item records associated with the given dictionary id.
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


}
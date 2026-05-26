package io.kudos.ms.sys.core.system.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.core.system.model.po.SysSystem
import io.kudos.ms.sys.core.system.model.table.SysSystems
import org.springframework.stereotype.Repository


/**
 * System DAO.
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysSystemDao : BaseCrudDao<String, SysSystem, SysSystems>() {

    /**
     * Queries by sub-system flag and returns the cache VO list.
     *
     * @param isSubSystem whether to fetch sub-systems
     * @return List<SysSystemCacheEntry>
     */
    open fun fetchSystemsByType(isSubSystem: Boolean): List<SysSystemCacheEntry> {
        val criteria = Criteria(SysSystem::subSystem eq isSubSystem)
        return searchAs<SysSystemCacheEntry>(criteria)
    }

}
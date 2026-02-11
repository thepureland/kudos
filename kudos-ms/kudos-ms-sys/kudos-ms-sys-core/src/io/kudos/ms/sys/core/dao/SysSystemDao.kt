package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ms.sys.core.model.po.SysSystem
import io.kudos.ms.sys.core.model.table.SysSystems
import org.springframework.stereotype.Repository


/**
 * 系统数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysSystemDao : BaseCrudDao<String, SysSystem, SysSystems>() {

    /**
     * 按是否子系统查询，返回缓存用 VO 列表
     *
     * @param isSubSystem 是否为子系统
     * @return List<SysSystemCacheItem>
     */
    open fun fetchSystemsByType(isSubSystem: Boolean): List<SysSystemCacheItem> {
        val criteria = Criteria(SysSystem::subSystem eq isSubSystem)
        return searchAs<SysSystemCacheItem>(criteria)
    }

}
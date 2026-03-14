package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.model.po.SysMicroService
import io.kudos.ms.sys.core.model.table.SysMicroServices
import org.springframework.stereotype.Repository


/**
 * 微服务数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysMicroServiceDao : BaseCrudDao<String, SysMicroService, SysMicroServices>() {

    /**
     * 按类型返回微服务，返回缓存用 VO 列表
     *
     * @param atomicService 是否原子服务
     * @return List<SysMicroServiceCacheEntry>
     */
    open fun fetchMicroServiceByTypeForCache(atomicService: Boolean): List<SysMicroServiceCacheEntry> {
        val criteria = Criteria(SysMicroService::atomicService eq atomicService)
        return searchAs<SysMicroServiceCacheEntry>(criteria)
    }

}
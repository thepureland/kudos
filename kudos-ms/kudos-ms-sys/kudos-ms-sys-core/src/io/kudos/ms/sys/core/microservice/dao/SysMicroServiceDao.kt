package io.kudos.ms.sys.core.microservice.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.model.po.SysMicroService
import io.kudos.ms.sys.core.microservice.model.table.SysMicroServices
import org.springframework.stereotype.Repository


/**
 * Micro-service DAO.
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysMicroServiceDao : BaseCrudDao<String, SysMicroService, SysMicroServices>() {

    /**
     * Return micro-services by type as a list of cache VOs.
     *
     * @param atomicService whether it is an atomic service
     * @return List<SysMicroServiceCacheEntry>
     */
    open fun fetchMicroServiceByTypeForCache(atomicService: Boolean): List<SysMicroServiceCacheEntry> {
        val criteria = Criteria(SysMicroService::atomicService eq atomicService)
        return searchAs<SysMicroServiceCacheEntry>(criteria)
    }

}
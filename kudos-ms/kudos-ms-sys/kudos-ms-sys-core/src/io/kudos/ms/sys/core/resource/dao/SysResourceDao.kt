package io.kudos.ms.sys.core.resource.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.core.resource.model.po.SysResource
import io.kudos.ms.sys.core.resource.model.table.SysResources
import org.springframework.stereotype.Repository

/**
 * Resource data access object
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysResourceDao : BaseCrudDao<String, SysResource, SysResources>() {

    /**
     * Query a resource by subsystem code + URL + active state (URL is non-null).
     *
     * @param subSystemCode subsystem code
     * @param url resource URL
     * @return SysResourceCacheEntry, or null if not found
     */
    open fun fetchResourceBySubSysAndUrl(subSystemCode: String, url: String): SysResourceCacheEntry? {
        val criteria = Criteria.and(
            SysResource::subSystemCode eq subSystemCode,
            SysResource::url eq url,
        )
        criteria.addAnd(SysResource::active eq true)
        return searchAs<SysResourceCacheEntry>(criteria).firstOrNull()
    }

    /**
     * Query resources by subsystem code + resource type code.
     *
     * @param subSystemCode subsystem code
     * @param resourceTypeDictCode resource type dictionary code
     * @return List<SysResourceCacheEntry>
     */
    open fun searchBySubSysAndType(subSystemCode: String, resourceTypeDictCode: String): List<SysResourceCacheEntry> {
        val criteria = Criteria.and(
            SysResource::subSystemCode eq subSystemCode,
            SysResource::resourceTypeDictCode eq resourceTypeDictCode,
            SysResource::active eq true,
        )
        return searchAs<SysResourceCacheEntry>(criteria)
    }

}
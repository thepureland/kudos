package io.kudos.ms.sys.core.resource.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.core.resource.model.po.SysResource
import io.kudos.ms.sys.core.resource.model.table.SysResources
import org.springframework.stereotype.Repository

/**
 * 资源数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysResourceDao : BaseCrudDao<String, SysResource, SysResources>() {

    /**
     * 按子系统编码+URL+启用状态查询资源（url 非空）
     *
     * @param subSystemCode 子系统编码
     * @param url 资源url
     * @return SysResourceCacheEntry，不存在返回null
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
     * 按子系统编码+资源类型代码查询资源列表
     *
     * @param subSystemCode 子系统编码
     * @param resourceTypeDictCode 资源类型字典码
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
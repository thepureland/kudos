package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.sys.core.model.po.SysResource
import io.kudos.ms.sys.core.model.table.SysResources
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
     * @return SysResourceCacheItem，不存在返回null
     */
    open fun fetchResourceBySubSysAndUrl(subSystemCode: String, url: String): SysResourceCacheItem? {
        val criteria = Criteria.and(
            SysResource::subSystemCode eq subSystemCode,
            SysResource::url eq url,
        )
        criteria.addAnd(SysResource::active eq null)
        return searchAs<SysResourceCacheItem>(criteria).firstOrNull()
    }

    /**
     * 按子系统编码+资源类型代码查询资源 id 列表
     *
     * @param subSystemCode 子系统编码
     * @param resourceTypeDictCode 资源类型字典码
     * @return List<资源ID>
     */
    open fun fetchResourceIdsBySubSysAndType(subSystemCode: String, resourceTypeDictCode: String): List<String> {
        val criteria = Criteria.and(
            SysResource::subSystemCode eq subSystemCode,
            SysResource::resourceTypeDictCode eq resourceTypeDictCode,
        )
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysResource::id.name) as List<String>
    }

}
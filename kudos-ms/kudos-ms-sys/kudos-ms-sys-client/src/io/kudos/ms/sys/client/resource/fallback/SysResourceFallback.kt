package io.kudos.ms.sys.client.resource.fallback

import io.kudos.ms.sys.client.resource.proxy.ISysResourceProxy
import io.kudos.ms.sys.client.support.SysClientFallbackSupport
import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import org.springframework.stereotype.Component


/**
 * Resource Feign fault-tolerant fallback implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysResourceFallback : SysClientFallbackSupport("SysResourceFallback"), ISysResourceProxy {

    override fun getResource(resourceId: String): SysResourceCacheEntry? {
        warnRead("getResource", resourceId)
        return null
    }

    override fun getResources(resourceIds: Collection<String>): Map<String, SysResourceCacheEntry> {
        warnRead("getResources", resourceIds)
        return emptyMap()
    }

    override fun getResources(
        resourceType: ResourceTypeEnum,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> {
        warnRead("getResources", resourceType, subSystemCode)
        return emptyList()
    }

    override fun getSimpleMenus(subSystemCode: String): List<BaseMenuTreeNode> {
        warnRead("getSimpleMenus", subSystemCode)
        return emptyList()
    }

    override fun getMenus(subSystemCode: String): List<MenuTreeNode> {
        warnRead("getMenus", subSystemCode)
        return emptyList()
    }

    override fun getResourceId(subSysDictCode: String, url: String): String? {
        warnRead("getResourceId", subSysDictCode, url)
        return null
    }

    override fun getDirectChildrenResources(
        resourceType: ResourceTypeEnum,
        parentId: String?,
        subSystemCode: String,
    ): List<SysResourceCacheEntry> {
        warnRead("getDirectChildrenResources", resourceType, parentId, subSystemCode)
        return emptyList()
    }

    override fun getChildrenResources(
        subSystemCode: String,
        resourceType: ResourceTypeEnum,
        parentId: String,
    ): List<SysResourceCacheEntry> {
        warnRead("getChildrenResources", subSystemCode, resourceType, parentId)
        return emptyList()
    }
}

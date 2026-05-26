package io.kudos.ms.auth.core.platform.api

import io.kudos.ms.auth.common.platform.api.IPermittedResource
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.ms.sys.common.resource.enums.ResourceTypeEnum
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import io.kudos.ms.user.common.passport.CurrentUserKit
import jakarta.annotation.Resource
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * Local implementation of [IPermittedResource].
 *
 * Marked [Primary] for the same reason as [io.kudos.ms.auth.core.role.api.AuthRoleApi]: when the
 * corresponding controller also implements the interface and is registered as a bean, this class
 * serves as the default to resolve the resulting injection ambiguity.
 *
 * @author K
 * @since 1.0.0
 */
@Primary
@Service
open class PermittedResourceApi : IPermittedResource {

    @Resource
    private lateinit var authRoleService: IAuthRoleService

    override fun getMenusForCurrentUser(): List<MenuTreeNode> {
        val userId = CurrentUserKit.currentUserIdOrNull() ?: return emptyList()
        val permitted = authRoleService.getResources(userId)
            .filter { it.resourceTypeDictCode == ResourceTypeEnum.MENU.code }
        if (permitted.isEmpty()) return emptyList()
        return buildMenuTree(permitted)
    }

    private fun buildMenuTree(resources: List<SysResourceCacheEntry>): List<MenuTreeNode> {
        val idSet = resources.mapTo(HashSet()) { it.id }
        val nodeMap = resources.associate { it.id to it.toNode() }
        val roots = mutableListOf<MenuTreeNode>()
        resources.sortedBy { it.orderNum ?: Int.MAX_VALUE }.forEach { item ->
            val node = nodeMap[item.id]!!
            val parentId = item.parentId
            // When the parent is not permitted, attach this menu as a root to avoid an orphaned hierarchy where a permitted child has a hidden parent.
            if (parentId.isNullOrBlank() || parentId !in idSet) {
                roots.add(node)
            } else {
                nodeMap[parentId]!!.children.add(node)
            }
        }
        return roots
    }

    private fun SysResourceCacheEntry.toNode(): MenuTreeNode = MenuTreeNode().also { node ->
        node.id = id
        node.title = name
        node.parentId = parentId
        node.seqNo = orderNum
        node.index = url
        node.icon = icon
    }
}

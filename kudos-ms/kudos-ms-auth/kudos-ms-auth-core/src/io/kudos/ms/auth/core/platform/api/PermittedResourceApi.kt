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

    /**
     * 把"用户被授权的菜单资源列表"装配成树。
     *
     * 关键设计：当父节点不在授权集合中（被裁掉了），该菜单**挂为根**而不是丢弃——
     * 避免"用户有子菜单权限但因父被裁出现悬空层级"导致整支菜单消失。
     *
     * @param resources 用户授权的资源列表（已通过 ResourceTypeEnum.MENU 过滤）
     * @return 菜单根节点列表
     * @author K
     * @since 1.0.0
     */
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

    /**
     * 把缓存条目 [SysResourceCacheEntry] 映射成 [MenuTreeNode]——仅复制菜单展示需要的几个字段。
     *
     * @return 菜单节点（children 由 [buildMenuTree] 后续挂载）
     * @author K
     * @since 1.0.0
     */
    private fun SysResourceCacheEntry.toNode(): MenuTreeNode = MenuTreeNode().also { node ->
        node.id = id
        node.title = name
        node.parentId = parentId
        node.seqNo = orderNum
        node.index = url
        node.icon = icon
    }
}

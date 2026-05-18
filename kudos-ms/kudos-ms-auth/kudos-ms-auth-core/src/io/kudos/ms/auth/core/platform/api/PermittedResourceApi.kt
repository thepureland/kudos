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
 * [IPermittedResource] 本地实现。
 *
 * 标 [Primary] 与 [io.kudos.ms.auth.core.role.api.AuthRoleApi] 同因：当对应 controller
 * 也实现接口注册为 bean 时，注入歧义由本类兜底。
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
            // 父节点未被授权时该菜单挂为根，避免出现"权限有孩子但隐藏父"的悬空层级
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

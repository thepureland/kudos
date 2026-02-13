package io.kudos.ms.auth.common.api

import io.kuark.service.sys.common.vo.resource.MenuTreeNode

/**
 * 被允许访问的资源接口
 *
 * @author K
 * @since 1.0.0
 */
interface IPermittedResource {

    /**
     * 获取当前用户有权限访问的菜单
     *
     * @return List<菜单树节点对象>
     */
    fun getMenusForCurrentUser(): List<MenuTreeNode>

}
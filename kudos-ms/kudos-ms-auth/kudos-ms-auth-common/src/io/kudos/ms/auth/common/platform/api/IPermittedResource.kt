package io.kudos.ms.auth.common.platform.api

import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import org.springframework.web.bind.annotation.GetMapping


/**
 * 被允许访问的资源接口。
 *
 * 当前实现读取 [io.kudos.ms.user.common.passport.CurrentUserKit] 中的登录态，结合
 * 角色↔资源链路返回该用户实际可访问的菜单树。未登录时返回空列表（HTTP 路径仍开放，
 * 调用方自行决定是否要求登录）。
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
    @GetMapping("/api/public/auth/permittedResource/getMenusForCurrentUser")
    fun getMenusForCurrentUser(): List<MenuTreeNode>

}

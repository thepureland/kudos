package io.kudos.ms.auth.common.platform.api

import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import org.springframework.web.bind.annotation.GetMapping


/**
 * Permitted-resource API.
 *
 * The current implementation reads the login state from [io.kudos.ms.user.common.passport.CurrentUserKit]
 * and, by following the role to resource chain, returns the menu tree the user may actually access.
 * Returns an empty list when not logged in (the HTTP path remains open; the caller decides whether
 * authentication is required).
 *
 * @author K
 * @since 1.0.0
 */
interface IPermittedResource {

    /**
     * Gets the menus the current user is permitted to access.
     *
     * @return List of menu tree node objects.
     */
    @GetMapping("/api/public/auth/permittedResource/getMenusForCurrentUser")
    fun getMenusForCurrentUser(): List<MenuTreeNode>

}

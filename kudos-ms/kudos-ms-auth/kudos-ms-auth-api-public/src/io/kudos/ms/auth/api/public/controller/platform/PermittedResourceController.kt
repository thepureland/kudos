package io.kudos.ms.auth.api.public.controller.platform

import io.kudos.ms.auth.common.platform.api.IPermittedResource
import io.kudos.ms.auth.core.platform.api.PermittedResourceApi
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import org.springframework.web.bind.annotation.RestController


/**
 * Controller exposing resources accessible to the current user. Paths are inherited from method-level annotations on [IPermittedResource].
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class PermittedResourceController(
    private val permittedResourceApi: PermittedResourceApi,
) : IPermittedResource {

    override fun getMenusForCurrentUser(): List<MenuTreeNode> =
        permittedResourceApi.getMenusForCurrentUser()

}

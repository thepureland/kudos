package io.kudos.ms.auth.api.public.controller.platform

import io.kudos.ms.auth.common.platform.api.IPermittedResource
import io.kudos.ms.auth.core.platform.api.PermittedResourceApi
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import org.springframework.web.bind.annotation.RestController


/**
 * 当前用户可访问资源 控制器。路径继承自 [IPermittedResource] 方法级注解。
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

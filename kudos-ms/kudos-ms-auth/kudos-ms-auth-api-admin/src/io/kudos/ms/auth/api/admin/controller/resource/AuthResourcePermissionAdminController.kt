package io.kudos.ms.auth.api.admin.controller.resource

import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * Resource / menu permission administration controller.
 *
 * Base URL: `/api/admin/auth/resourcepermission`
 *
 * The resource and menu data themselves (rows, tree, pagination) are owned by kudos-ms-sys and read
 * through its `/api/admin/sys/resource/...` endpoints. This controller only supplies the role↔resource
 * projection the permission console overlays on those rows: given the ids of the resources currently
 * on screen, the names of the roles granted each one.
 *
 *   POST /roleNamesByResourceIds   body=[resourceIds]   → Map<resourceId, List<roleName>>
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/resourcepermission")
class AuthResourcePermissionAdminController(
    private val authRoleService: IAuthRoleService,
) {

    /**
     * Batch-resolve the granting roles for a page of resources. Resources with no roles are omitted
     * from the response. Body is a JSON array of resource ids.
     */
    @PostMapping("/roleNamesByResourceIds")
    fun roleNamesByResourceIds(@RequestBody resourceIds: List<String>): Map<String, List<String>> =
        authRoleService.getRoleNamesByResourceIds(resourceIds)

}

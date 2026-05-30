package io.kudos.ms.auth.api.admin.controller.role

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.auth.common.exclusion.vo.request.AuthRoleExclusionFormCreate
import io.kudos.ms.auth.common.exclusion.vo.request.AuthRoleExclusionFormUpdate
import io.kudos.ms.auth.common.exclusion.vo.request.AuthRoleExclusionQuery
import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionDetail
import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionRow
import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionViolationVo
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion
import io.kudos.ms.auth.core.role.exclusion.service.iservice.IAuthRoleExclusionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * SoD mutual-exclusion pair administration controller.
 *
 * Exposes CRUD for [AuthRoleExclusion] plus a violation-scan endpoint that lets admins discover
 * users who currently hold both sides of an exclusion pair.
 *
 * Base URL: `/api/admin/auth/roleExclusion`
 *
 * **Why a separate controller (not merged into AuthRoleAdminController)?**
 * Exclusion management is conceptually a cross-cutting admin concern, not a sub-operation of a
 * single role. Keeping it separate keeps `AuthRoleAdminController` focused and makes the
 * exclusion endpoints independently discoverable via API docs.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/roleExclusion")
class AuthRoleExclusionAdminController :
    BaseCrudController<
        String,
        IAuthRoleExclusionService,
        AuthRoleExclusionQuery,
        AuthRoleExclusionRow,
        AuthRoleExclusionDetail,
        AuthRoleExclusionDetail, // edit VO = detail (only description is mutable)
        AuthRoleExclusionFormCreate,
        AuthRoleExclusionFormUpdate,
    >() {

    /**
     * Returns the list of users who currently violate the exclusion identified by [id].
     * A violation means the user simultaneously holds both [AuthRoleExclusion.roleAId] and
     * [AuthRoleExclusion.roleBId] via any path (direct, group, or parent-chain inheritance).
     *
     * Intended for a periodic SoD compliance dashboard, NOT for the hot bind path.
     */
    @GetMapping("/findViolations")
    fun findViolations(@RequestParam id: String): AuthRoleExclusionViolationVo =
        service.findViolatingUserIds(id)
}

package io.kudos.ms.auth.api.admin.controller.version

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * Token freshness: read a principal's permission version, or force every token they hold to expire.
 *
 * The second one is the endpoint an operator reaches for at the worst moment — a leaked credential,
 * a departure at speed — so it is deliberately one call with no prerequisites, and it is audited.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/version")
class PermissionVersionAdminController {

    @Resource
    private lateinit var permissionVersionApi: IPermissionVersionApi

    /**
     * The principal's current permission version — what a token minted right now would carry.
     *
     * Exposed mainly for diagnosis: when somebody reports being logged out, comparing this against
     * the version in their token says immediately whether that was a revocation or something else.
     */
    @GetMapping("/current")
    fun current(
        @RequestParam userId: String,
        @RequestParam(required = false, defaultValue = "USER") principalType: String,
    ): String = permissionVersionApi.currentVersion(SubjectRef(userId, principalType))

    /**
     * Forces every token this principal holds to be stale, immediately.
     *
     * @return the new epoch
     */
    @PostMapping("/revokeAllTokens")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "强制主体所有令牌失效")
    fun revokeAllTokens(
        @RequestParam userId: String,
        @RequestParam(required = false) reason: String?,
    ): Long = permissionVersionApi.revokeAllTokens(userId, reason)

    companion object {
        private const val MODULE_CODE = "auth-version"
    }
}

package io.kudos.ms.auth.api.admin.controller.authz

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.common.authz.vo.response.PermissionExplanationVo
import io.kudos.ms.auth.core.platform.authz.PermissionExplainService
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * The "why" endpoints.
 *
 * Every other admin screen answers *what* a subject can do. These answer *why*, which is the
 * question that actually gets asked when something is wrong — and the one that, without an endpoint,
 * gets answered by reading the role tree by hand.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/authz")
class AuthzExplainAdminController {

    @Resource
    private lateinit var permissionExplainService: PermissionExplainService

    @Resource
    private lateinit var authzDecisionApi: IAuthzDecisionApi

    /**
     * Why [userId] does or does not hold [permissionCode]: the decision, the rule that settled it,
     * and every binding of theirs that addresses the code.
     *
     * [clientIp] is accepted so a conditional binding can be explained as the real caller would
     * experience it — "it works for them but not for me" is usually a condition, and an explanation
     * evaluated without context would quietly say the wrong thing.
     */
    @GetMapping("/explain")
    fun explain(
        @RequestParam userId: String,
        @RequestParam permissionCode: String,
        @RequestParam(required = false) clientIp: String?,
    ): PermissionExplanationVo {
        val context = if (clientIp.isNullOrBlank()) emptyMap() else mapOf<String, Any?>("ip" to clientIp)
        return permissionExplainService.explain(userId, permissionCode, context)
    }

    /** Every permission code a subject holds, merged (DENY removed). For debugging and for tooling. */
    @GetMapping("/listPermissionCodes")
    fun listPermissionCodes(@RequestParam userId: String): Set<String> =
        authzDecisionApi.permissionCodes(SubjectRef.ofUser(userId))
}

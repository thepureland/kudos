package io.kudos.ms.auth.api.admin.controller.instance

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ms.auth.common.instance.vo.InstanceGrantVo
import io.kudos.ms.auth.common.instance.vo.InstanceShareRequest
import io.kudos.ms.auth.core.instance.service.iservice.IAuthInstanceGrantService
import io.kudos.ms.user.common.passport.CurrentUserKit
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * Point-to-point shares: "this person may act on this particular row".
 *
 * **Why an admin surface exists for something applications drive.** Sharing itself is an application
 * action — a document app calls the service directly when somebody presses Share. What has no other
 * home is the *oversight*: instance grants sit outside the role model on purpose, so every role
 * report is blind to them. Without [listSharesOfPrincipal], "what does this person have access to"
 * has an answer that is quietly incomplete, and an offboarding review misses precisely the access
 * that was granted informally.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/instanceGrant")
class AuthInstanceGrantAdminController {

    @Resource
    private lateinit var authInstanceGrantService: IAuthInstanceGrantService

    /** Everyone a particular instance is currently shared with — the sharing dialog's list. */
    @GetMapping("/listShares")
    fun listShares(
        @RequestParam resourceType: String,
        @RequestParam instanceId: String,
    ): List<InstanceGrantVo> = authInstanceGrantService.listShareVos(resourceType, instanceId)

    /**
     * Everything currently shared **with** a principal — the direction no role report can produce.
     */
    @GetMapping("/listSharesOfPrincipal")
    fun listSharesOfPrincipal(
        @RequestParam principalId: String,
        @RequestParam(required = false) resourceType: String?,
    ): List<InstanceGrantVo> = authInstanceGrantService.listSharesOfPrincipal(principalId, resourceType)

    /**
     * Shares an instance, or updates the existing share for the same tuple.
     *
     * The operator is taken from the login state rather than the request body: `grantedBy` is what
     * an audit reads to find who handed access out, and a value the caller supplies is a value the
     * caller can choose.
     */
    @PostMapping("/share")
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "分享实例权限")
    fun share(@RequestBody request: InstanceShareRequest): String =
        authInstanceGrantService.share(
            principalId = request.principalId,
            tenantId = request.tenantId,
            resourceType = request.resourceType,
            instanceId = request.instanceId,
            action = request.action,
            effect = request.effect,
            startTime = request.startTime,
            endTime = request.endTime,
            grantedBy = CurrentUserKit.currentUserIdOrNull(),
            principalType = request.principalType,
        )

    /** Withdraws a share. Soft, so an un-share stays answerable afterwards. */
    @DeleteMapping("/unshare")
    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "撤销实例分享")
    fun unshare(
        @RequestParam grantId: String,
        @RequestParam(required = false) reason: String?,
    ): Boolean = authInstanceGrantService.unshare(grantId, reason)

    companion object {
        private const val MODULE_CODE = "auth-instance-grant"
    }
}

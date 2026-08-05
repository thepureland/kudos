package io.kudos.ms.auth.api.admin.controller.grant

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.web.springmvc.controller.BaseReadOnlyController
import io.kudos.ms.auth.common.grant.vo.request.AuthRoleGrantDecisionRequest
import io.kudos.ms.auth.common.grant.vo.request.AuthRoleGrantRequestQuery
import io.kudos.ms.auth.common.grant.vo.request.AuthRoleGrantSubmitRequest
import io.kudos.ms.auth.common.grant.vo.response.AuthRoleGrantRequestDetail
import io.kudos.ms.auth.common.grant.vo.response.AuthRoleGrantRequestRow
import io.kudos.ms.auth.core.role.grant.service.iservice.IAuthRoleGrantRequestService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Admin controller for the role-grant approval workflow.
 *
 * Base URL: `/api/admin/auth/roleGrantRequest`
 *
 * Read side (inherited from [BaseReadOnlyController]):
 *   POST /pagingSearch  → page of AuthRoleGrantRequestRow (approver dashboard, filterable by status)
 *   GET  /getDetail     → AuthRoleGrantRequestDetail
 *
 * Write side (the workflow actions): submit / approve / reject / cancel.
 *
 * Approver authorisation is enforced by access to this controller — there is no per-request
 * approver routing in the data model (a follow-up could add an approver-role concept).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/auth/roleGrantRequest")
class AuthRoleGrantRequestAdminController :
    BaseReadOnlyController<
        String,
        IAuthRoleGrantRequestService,
        AuthRoleGrantRequestQuery,
        AuthRoleGrantRequestRow,
        AuthRoleGrantRequestDetail,
        >() {

    /** Submit a new grant request. Returns the new request id. */
    @PostMapping("/submit")
    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "提交角色授权申请")
    fun submit(@RequestBody request: AuthRoleGrantSubmitRequest): String =
        service.submit(request.roleId, request.userId, request.reason)

    /** Approve a pending request (performs the actual bind, subject to SoD checks). */
    @PostMapping("/approve")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "批准角色授权申请")
    fun approve(@RequestBody request: AuthRoleGrantDecisionRequest): Boolean =
        service.approve(request.id, request.comment)

    /** Reject a pending request. */
    @PostMapping("/reject")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "驳回角色授权申请")
    fun reject(@RequestBody request: AuthRoleGrantDecisionRequest): Boolean =
        service.reject(request.id, request.comment)

    /** Cancel a pending request (requester action). */
    @PostMapping("/cancel")
    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "撤回角色授权申请")
    fun cancel(@RequestBody request: AuthRoleGrantDecisionRequest): Boolean =
        service.cancel(request.id)

    companion object {
        /** 审计日志的归属模块编码 */
        private const val MODULE_CODE = "auth-role-grant-request"
    }
}

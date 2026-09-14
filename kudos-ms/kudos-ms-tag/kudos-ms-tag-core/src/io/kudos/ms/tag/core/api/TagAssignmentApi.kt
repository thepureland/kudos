package io.kudos.ms.tag.core.api

import io.kudos.ms.tag.common.assignment.api.ITagAssignmentApi
import io.kudos.ms.tag.common.assignment.model.AssignManualTagRequest
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentResponse
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentStatus
import io.kudos.ms.tag.common.assignment.model.RemoveManualTagRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculateSubjectsRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculationResult
import io.kudos.ms.tag.core.assignment.model.AssignManualTagCommand
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentResult
import io.kudos.ms.tag.core.assignment.model.RemoveManualTagCommand
import io.kudos.ms.tag.core.assignment.service.iservice.ITagAssignmentService
import io.kudos.ms.tag.core.runtime.service.iservice.ITagRecalculationService
import io.kudos.ms.tag.core.runtime.service.iservice.RecalculationSummary
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
open class TagAssignmentApi(
    private val assignmentService: ITagAssignmentService,
    private val recalculationService: ITagRecalculationService,
    private val tenantAccessGuard: TagTenantAccessGuard,
) : ITagAssignmentApi {
    override fun assignManual(request: AssignManualTagRequest): ManualTagAssignmentResponse {
        tenantAccessGuard.requireTenant(request.subjectKey.tenantId)
        return assignmentService.assignManual(
            AssignManualTagCommand(
                request.eventId,
                request.requestId,
                request.subjectKey,
                request.tagId,
                request.operatorId,
                request.operatorName,
                request.reason,
                request.effectiveUntil,
                request.occurredAt,
            )
        ).toApiResponse()
    }

    override fun removeManual(request: RemoveManualTagRequest): ManualTagAssignmentResponse {
        tenantAccessGuard.requireTenant(request.subjectKey.tenantId)
        return assignmentService.removeManual(
            RemoveManualTagCommand(
                request.eventId,
                request.requestId,
                request.subjectKey,
                request.tagId,
                request.manualSourceRef,
                request.operatorId,
                request.operatorName,
                request.reason,
                request.occurredAt,
            )
        ).toApiResponse()
    }

    override fun recalculateSubjects(request: TagRecalculateSubjectsRequest): TagRecalculationResult {
        request.subjectKeys.map { it.tenantId }.distinct().forEach(tenantAccessGuard::requireTenant)
        return recalculationService.recalculateSubjectNow(request.subjectKeys, request.directRuleLimit).toApiResult()
    }
}

private fun ManualAssignmentResult.toApiResponse() = ManualTagAssignmentResponse(
    eventId = eventId,
    status = ManualTagAssignmentStatus.valueOf(status.name),
    assignedTagIds = delta.assignedTagIds,
    removedTagIds = delta.removedTagIds,
    assignmentVersion = delta.assignmentVersion,
)

private fun RecalculationSummary.toApiResult() = TagRecalculationResult(
    subjectsProcessed,
    directRulesEvaluated,
    cascadedRulesEvaluated,
    assignmentChanges,
)

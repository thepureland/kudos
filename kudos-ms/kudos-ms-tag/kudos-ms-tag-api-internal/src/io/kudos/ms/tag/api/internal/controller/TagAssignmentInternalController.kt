package io.kudos.ms.tag.api.internal.controller

import io.kudos.ms.tag.common.assignment.api.ITagAssignmentApi
import io.kudos.ms.tag.common.assignment.model.AssignManualTagRequest
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentResponse
import io.kudos.ms.tag.common.assignment.model.RemoveManualTagRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculateSubjectsRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculationResult
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.RestController

@RestController
open class TagAssignmentInternalController(
    @Qualifier("tagAssignmentApi") private val delegate: ITagAssignmentApi,
    private val tenantAccessGuard: TagTenantAccessGuard,
) : ITagAssignmentApi {
    override fun assignManual(request: AssignManualTagRequest): ManualTagAssignmentResponse {
        tenantAccessGuard.requireTenant(request.subjectKey.tenantId)
        return delegate.assignManual(request)
    }

    override fun removeManual(request: RemoveManualTagRequest): ManualTagAssignmentResponse {
        tenantAccessGuard.requireTenant(request.subjectKey.tenantId)
        return delegate.removeManual(request)
    }

    override fun recalculateSubjects(request: TagRecalculateSubjectsRequest): TagRecalculationResult {
        request.subjectKeys.map { it.tenantId }.distinct().forEach(tenantAccessGuard::requireTenant)
        return delegate.recalculateSubjects(request)
    }
}

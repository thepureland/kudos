package io.kudos.ms.tag.client.fallback

import io.kudos.ms.tag.client.proxy.ITagAssignmentProxy
import io.kudos.ms.tag.common.assignment.model.AssignManualTagRequest
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentResponse
import io.kudos.ms.tag.common.assignment.model.RemoveManualTagRequest
import io.kudos.ms.tag.common.error.TagServiceUnavailableException
import io.kudos.ms.tag.common.runtime.model.TagRecalculateSubjectsRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculationResult

open class TagAssignmentFallback : ITagAssignmentProxy {
    fun assignManual(cause: Throwable, request: AssignManualTagRequest): ManualTagAssignmentResponse =
        unavailable("assignManual", cause)

    override fun assignManual(request: AssignManualTagRequest): ManualTagAssignmentResponse =
        unavailable("assignManual", null)

    fun removeManual(cause: Throwable, request: RemoveManualTagRequest): ManualTagAssignmentResponse =
        unavailable("removeManual", cause)

    override fun removeManual(request: RemoveManualTagRequest): ManualTagAssignmentResponse =
        unavailable("removeManual", null)

    fun recalculateSubjects(cause: Throwable, request: TagRecalculateSubjectsRequest): TagRecalculationResult =
        unavailable("recalculateSubjects", cause)

    override fun recalculateSubjects(request: TagRecalculateSubjectsRequest): TagRecalculationResult =
        unavailable("recalculateSubjects", null)

    private fun <T> unavailable(operation: String, cause: Throwable?): T =
        throw TagServiceUnavailableException(operation, cause)
}

package io.kudos.ms.tag.common.assignment.api

import io.kudos.ms.tag.common.assignment.model.AssignManualTagRequest
import io.kudos.ms.tag.common.assignment.model.ManualTagAssignmentResponse
import io.kudos.ms.tag.common.assignment.model.RemoveManualTagRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculateSubjectsRequest
import io.kudos.ms.tag.common.runtime.model.TagRecalculationResult
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.DeleteExchange
import org.springframework.web.service.annotation.PostExchange

/** Shared local/remote contract for manual membership and bounded synchronous recalculation. */
interface ITagAssignmentApi {
    @PostExchange("/api/internal/tag/assignments/manual")
    fun assignManual(@RequestBody request: AssignManualTagRequest): ManualTagAssignmentResponse

    @DeleteExchange("/api/internal/tag/assignments/manual")
    fun removeManual(@RequestBody request: RemoveManualTagRequest): ManualTagAssignmentResponse

    @PostExchange("/api/internal/tag/recalculate/subjects")
    fun recalculateSubjects(@RequestBody request: TagRecalculateSubjectsRequest): TagRecalculationResult
}

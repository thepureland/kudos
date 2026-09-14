package io.kudos.ms.tag.core.assignment.service.iservice

import io.kudos.ms.tag.core.assignment.model.AssignManualTagCommand
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentResult
import io.kudos.ms.tag.core.assignment.model.RemoveManualTagCommand
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.AssignmentDelta

interface ITagAssignmentService {
    fun assignManual(command: AssignManualTagCommand): ManualAssignmentResult
    fun removeManual(command: RemoveManualTagCommand): ManualAssignmentResult
    fun applyRuleResult(
        key: TagSubjectKey,
        tagId: String,
        ruleId: String,
        ruleVersion: Long,
        matched: Boolean,
        causeRef: String,
    ): AssignmentDelta
}

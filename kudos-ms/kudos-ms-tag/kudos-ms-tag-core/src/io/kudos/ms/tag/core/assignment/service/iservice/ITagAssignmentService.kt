package io.kudos.ms.tag.core.assignment.service.iservice

import io.kudos.ms.tag.core.assignment.model.AssignManualTagCommand
import io.kudos.ms.tag.core.assignment.model.ManualAssignmentResult
import io.kudos.ms.tag.core.assignment.model.RemoveManualTagCommand

interface ITagAssignmentService {
    fun assignManual(command: AssignManualTagCommand): ManualAssignmentResult
    fun removeManual(command: RemoveManualTagCommand): ManualAssignmentResult
}

package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.AssignmentDelta
import io.kudos.ms.tag.core.runtime.port.AssignmentSearchResult
import io.kudos.ms.tag.core.runtime.port.ResolvedAssignment
import io.kudos.ms.tag.core.runtime.port.TagAssignmentIndex
import io.kudos.ms.tag.core.runtime.port.TagKeysetPage

/** Default RDB adapter boundary; materialized querying is implemented in Task 11. */
open class RdbTagAssignmentIndex : TagAssignmentIndex {
    override fun replaceForSet(
        key: TagSubjectKey,
        tagSetId: String?,
        winners: List<ResolvedAssignment>,
    ): AssignmentDelta = error("RDB assignment mutation is not initialized yet.")

    override fun search(
        tenantId: String,
        subjectType: String,
        expression: TagQueryExpression,
        page: TagKeysetPage,
    ): AssignmentSearchResult = error("RDB assignment search is not initialized yet.")
}

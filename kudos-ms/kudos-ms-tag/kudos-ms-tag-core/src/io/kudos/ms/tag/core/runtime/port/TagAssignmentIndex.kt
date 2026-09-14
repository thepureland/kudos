package io.kudos.ms.tag.core.runtime.port

import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.subject.model.TagSubjectKey

interface TagAssignmentIndex {
    fun replaceForSet(key: TagSubjectKey, tagSetId: String?, winners: List<ResolvedAssignment>): AssignmentDelta
    fun search(
        tenantId: String,
        subjectType: String,
        expression: TagQueryExpression,
        page: TagKeysetPage,
    ): AssignmentSearchResult
}

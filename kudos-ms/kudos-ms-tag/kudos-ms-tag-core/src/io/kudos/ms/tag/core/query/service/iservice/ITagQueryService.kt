package io.kudos.ms.tag.core.query.service.iservice

import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage

interface ITagQueryService {
    fun findSubjects(request: TagQueryRequest): TagSubjectPage
}

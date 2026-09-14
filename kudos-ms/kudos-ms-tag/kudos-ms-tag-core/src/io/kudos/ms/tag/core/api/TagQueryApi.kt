package io.kudos.ms.tag.core.api

import io.kudos.ms.tag.common.query.api.ITagQueryApi
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage
import io.kudos.ms.tag.core.query.service.iservice.ITagQueryService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
open class TagQueryApi(
    private val service: ITagQueryService,
) : ITagQueryApi {
    override fun findSubjects(request: TagQueryRequest): TagSubjectPage = service.findSubjects(request)
}

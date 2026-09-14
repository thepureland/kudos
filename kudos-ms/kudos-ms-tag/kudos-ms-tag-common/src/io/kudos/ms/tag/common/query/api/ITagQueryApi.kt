package io.kudos.ms.tag.common.query.api

import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

/** Shared local/remote contract for querying materialized tag assignments. */
interface ITagQueryApi {
    @PostExchange("/api/internal/tag/query")
    fun findSubjects(@RequestBody request: TagQueryRequest): TagSubjectPage
}

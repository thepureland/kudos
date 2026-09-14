package io.kudos.ms.tag.common.fact.api

import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.fact.model.TagAttributeFactResult
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange

/** Shared local/remote contract for submitting idempotent subject facts. */
interface ITagAttributeFactApi {
    @PostExchange("/api/internal/tag/facts")
    fun submitFacts(@RequestBody facts: List<TagAttributeFact>): List<TagAttributeFactResult>
}

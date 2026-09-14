package io.kudos.ms.tag.core.api

import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.fact.api.ITagAttributeFactApi
import io.kudos.ms.tag.common.fact.model.TagAttributeFactResult
import io.kudos.ms.tag.common.fact.model.TagAttributeFactStatus
import io.kudos.ms.tag.core.fact.model.AttributeFactResult
import io.kudos.ms.tag.core.fact.service.iservice.ITagAttributeFactService
import io.kudos.ms.tag.core.security.TagSubjectWriteGuard
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
open class TagAttributeFactApi(
    private val service: ITagAttributeFactService,
    private val writeGuard: TagSubjectWriteGuard,
) : ITagAttributeFactApi {
    override fun submitFacts(facts: List<TagAttributeFact>): List<TagAttributeFactResult> {
        facts.forEach { writeGuard.requireWrite(it.subjectKey) }
        return service.submitFacts(facts).map(AttributeFactResult::toApiResult)
    }
}

private fun AttributeFactResult.toApiResult() = TagAttributeFactResult(
    eventId = eventId,
    status = TagAttributeFactStatus.valueOf(status.name),
    stateVersion = stateVersion,
    errorCode = errorCode,
    errorMessage = errorMessage,
)

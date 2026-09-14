package io.kudos.ms.tag.api.internal.controller

import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.fact.api.ITagAttributeFactApi
import io.kudos.ms.tag.common.fact.model.TagAttributeFactResult
import io.kudos.ms.tag.core.security.TagSubjectWriteGuard
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.RestController

@RestController
open class TagAttributeFactInternalController(
    @Qualifier("tagAttributeFactApi") private val delegate: ITagAttributeFactApi,
    private val writeGuard: TagSubjectWriteGuard,
) : ITagAttributeFactApi {
    override fun submitFacts(facts: List<TagAttributeFact>): List<TagAttributeFactResult> {
        facts.forEach { writeGuard.requireWrite(it.subjectKey) }
        return delegate.submitFacts(facts)
    }
}

package io.kudos.ms.tag.core.fact.service.iservice

import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.core.fact.model.AttributeFactResult

interface ITagAttributeFactService {
    fun submitFact(fact: TagAttributeFact): AttributeFactResult
    fun submitFacts(facts: List<TagAttributeFact>, sliceSize: Int = 100): List<AttributeFactResult>
}

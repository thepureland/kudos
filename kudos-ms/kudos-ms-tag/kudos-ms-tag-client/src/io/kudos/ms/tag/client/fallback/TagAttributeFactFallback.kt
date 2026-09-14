package io.kudos.ms.tag.client.fallback

import io.kudos.ms.tag.client.proxy.ITagAttributeFactProxy
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.error.TagServiceUnavailableException
import io.kudos.ms.tag.common.fact.model.TagAttributeFactResult

open class TagAttributeFactFallback : ITagAttributeFactProxy {
    fun submitFacts(cause: Throwable, facts: List<TagAttributeFact>): List<TagAttributeFactResult> =
        unavailable("submitFacts", cause)

    override fun submitFacts(facts: List<TagAttributeFact>): List<TagAttributeFactResult> =
        unavailable("submitFacts", null)
}

private fun <T> unavailable(operation: String, cause: Throwable?): T =
    throw TagServiceUnavailableException(operation, cause)

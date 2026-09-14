package io.kudos.ms.tag.core.runtime.port

import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.TagAttributeView
import io.kudos.ms.tag.common.subject.model.TagSubjectKey

interface AttributeStateStore {
    fun apply(fact: TagAttributeFact, definition: TagAttributeView): AttributeApplyResult
    fun load(key: TagSubjectKey, attributeCodes: Set<String>): Map<String, List<TagAttributeValue>>
}

package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.TagAttributeView
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.AttributeApplyResult
import io.kudos.ms.tag.core.runtime.port.AttributeStateStore

/** Default RDB adapter boundary; state mutation is implemented in Task 8. */
open class RdbAttributeStateStore : AttributeStateStore {
    override fun apply(fact: TagAttributeFact, definition: TagAttributeView): AttributeApplyResult =
        error("RDB attribute state mutation is not initialized yet.")

    override fun load(key: TagSubjectKey, attributeCodes: Set<String>): Map<String, List<TagAttributeValue>> =
        error("RDB attribute state loading is not initialized yet.")
}

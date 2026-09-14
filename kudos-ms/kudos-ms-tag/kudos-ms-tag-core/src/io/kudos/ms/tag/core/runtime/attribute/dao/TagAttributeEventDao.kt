package io.kudos.ms.tag.core.runtime.attribute.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.attribute.model.po.TagAttributeEvent
import io.kudos.ms.tag.core.runtime.attribute.model.table.TagAttributeEvents
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagAttributeEventDao : BaseCrudDao<String, TagAttributeEvent, TagAttributeEvents>() {
    open fun latestAppliedSourceVersion(key: TagSubjectKey, attributeId: String): Long? =
        entitySequence().filter {
            (TagAttributeEvents.tenantId eq key.tenantId) and
                (TagAttributeEvents.subjectType eq key.subjectType) and
                (TagAttributeEvents.subjectId eq key.subjectId) and
                (TagAttributeEvents.attributeId eq attributeId) and
                (TagAttributeEvents.processStatus eq "APPLIED")
        }.toList()
            .asSequence()
            .filter { it.operation == "SET" || it.operation == "CLEAR" }
            .mapNotNull { it.sourceVersion }
            .maxOrNull()
}

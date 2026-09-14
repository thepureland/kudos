package io.kudos.ms.tag.core.runtime.assignment.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignmentEvent
import io.kudos.ms.tag.core.runtime.assignment.model.table.TagAssignmentEvents
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagAssignmentEventDao : BaseCrudDao<String, TagAssignmentEvent, TagAssignmentEvents>() {
    open fun list(key: TagSubjectKey): List<TagAssignmentEvent> = entitySequence().filter {
        (TagAssignmentEvents.tenantId eq key.tenantId) and
            (TagAssignmentEvents.subjectType eq key.subjectType) and
            (TagAssignmentEvents.subjectId eq key.subjectId)
    }.toList()
}

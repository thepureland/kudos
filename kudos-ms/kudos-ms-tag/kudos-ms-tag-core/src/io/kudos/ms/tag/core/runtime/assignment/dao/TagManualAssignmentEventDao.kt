package io.kudos.ms.tag.core.runtime.assignment.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagManualAssignmentEvent
import io.kudos.ms.tag.core.runtime.assignment.model.table.TagManualAssignmentEvents
import org.springframework.stereotype.Repository

@Repository
open class TagManualAssignmentEventDao :
    BaseCrudDao<String, TagManualAssignmentEvent, TagManualAssignmentEvents>()

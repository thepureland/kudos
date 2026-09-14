package io.kudos.ms.tag.core.runtime.assignment.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignmentEvent
import io.kudos.ms.tag.core.runtime.assignment.model.table.TagAssignmentEvents
import org.springframework.stereotype.Repository

@Repository
open class TagAssignmentEventDao : BaseCrudDao<String, TagAssignmentEvent, TagAssignmentEvents>()

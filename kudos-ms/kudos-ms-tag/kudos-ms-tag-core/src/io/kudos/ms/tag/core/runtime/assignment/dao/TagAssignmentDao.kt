package io.kudos.ms.tag.core.runtime.assignment.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignment
import io.kudos.ms.tag.core.runtime.assignment.model.table.TagAssignments
import org.springframework.stereotype.Repository

@Repository
open class TagAssignmentDao : BaseCrudDao<String, TagAssignment, TagAssignments>()

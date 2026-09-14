package io.kudos.ms.tag.core.runtime.assignment.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignment
import io.kudos.ms.tag.core.runtime.assignment.model.table.TagAssignments
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import org.ktorm.dsl.and
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.isNull
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagAssignmentDao : BaseCrudDao<String, TagAssignment, TagAssignments>() {
    open fun list(key: TagSubjectKey): List<TagAssignment> = entitySequence().filter {
        (TagAssignments.tenantId eq key.tenantId) and
            (TagAssignments.subjectType eq key.subjectType) and
            (TagAssignments.subjectId eq key.subjectId)
    }.toList()

    open fun listForSet(key: TagSubjectKey, tagSetId: String?): List<TagAssignment> = entitySequence().filter {
        val keyCondition = (TagAssignments.tenantId eq key.tenantId) and
            (TagAssignments.subjectType eq key.subjectType) and
            (TagAssignments.subjectId eq key.subjectId)
        keyCondition and if (tagSetId == null) TagAssignments.exclusiveSetId.isNull() else TagAssignments.exclusiveSetId eq tagSetId
    }.toList()

    open fun insertAssignment(assignment: TagAssignment): Boolean = database().insert(TagAssignments) {
        set(TagAssignments.tagId, assignment.tagId)
        set(TagAssignments.tenantId, assignment.tenantId)
        set(TagAssignments.subjectType, assignment.subjectType)
        set(TagAssignments.subjectId, assignment.subjectId)
        set(TagAssignments.exclusiveSetId, assignment.exclusiveSetId)
        set(TagAssignments.assignmentVersion, assignment.assignmentVersion)
        set(TagAssignments.evaluatedRuleVersion, assignment.evaluatedRuleVersion)
        set(TagAssignments.materializedTime, assignment.materializedTime)
        set(TagAssignments.effectiveFrom, assignment.effectiveFrom)
        set(TagAssignments.effectiveUntil, assignment.effectiveUntil)
        set(TagAssignments.updateTime, assignment.updateTime)
    } == 1

    open fun deleteAssignment(key: TagSubjectKey, tagId: String): Boolean = database().delete(TagAssignments) {
        (TagAssignments.tenantId eq key.tenantId) and
            (TagAssignments.subjectType eq key.subjectType) and
            (TagAssignments.subjectId eq key.subjectId) and
            (TagAssignments.tagId eq tagId)
    } == 1
}

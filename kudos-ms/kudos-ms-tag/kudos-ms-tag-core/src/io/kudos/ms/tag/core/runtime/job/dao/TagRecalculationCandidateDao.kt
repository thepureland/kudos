package io.kudos.ms.tag.core.runtime.job.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationCandidate
import io.kudos.ms.tag.core.runtime.job.model.table.TagRecalculationCandidates
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagRecalculationCandidateDao :
    BaseCrudDao<String, TagRecalculationCandidate, TagRecalculationCandidates>() {

    open fun insertCandidate(candidate: TagRecalculationCandidate): Boolean {
        val exists = entitySequence().filter {
            (TagRecalculationCandidates.runId eq candidate.runId) and
                (TagRecalculationCandidates.subjectType eq candidate.subjectType) and
                (TagRecalculationCandidates.subjectId eq candidate.subjectId) and
                (TagRecalculationCandidates.tagId eq candidate.tagId)
        }.toList().isNotEmpty()
        if (exists) return false
        return database().insert(TagRecalculationCandidates) {
            set(TagRecalculationCandidates.runId, candidate.runId)
            set(TagRecalculationCandidates.tenantId, candidate.tenantId)
            set(TagRecalculationCandidates.subjectType, candidate.subjectType)
            set(TagRecalculationCandidates.subjectId, candidate.subjectId)
            set(TagRecalculationCandidates.tagId, candidate.tagId)
            set(TagRecalculationCandidates.ruleVersion, candidate.ruleVersion)
            set(TagRecalculationCandidates.evaluatedTime, candidate.evaluatedTime)
        } == 1
    }

    open fun listByRun(runId: String): List<TagRecalculationCandidate> =
        entitySequence().filter { TagRecalculationCandidates.runId eq runId }
            .sortedBy { TagRecalculationCandidates.subjectId }
            .toList()
}

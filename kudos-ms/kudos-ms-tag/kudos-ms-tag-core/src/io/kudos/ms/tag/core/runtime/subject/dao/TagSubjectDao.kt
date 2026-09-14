package io.kudos.ms.tag.core.runtime.subject.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.subject.model.po.TagSubject
import io.kudos.ms.tag.core.runtime.subject.model.table.TagSubjects
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.firstOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
open class TagSubjectDao : BaseCrudDao<String, TagSubject, TagSubjects>() {
    open fun find(key: TagSubjectKey): TagSubject? = entitySequence().firstOrNull {
        (TagSubjects.tenantId eq key.tenantId) and
            (TagSubjects.subjectType eq key.subjectType) and
            (TagSubjects.subjectId eq key.subjectId)
    }

    /** Inserts a subject without asking Ktorm to generate a key for the composite primary key. */
    open fun insertSubject(subject: TagSubject): Boolean = database().insert(TagSubjects) {
        set(TagSubjects.subjectId, subject.subjectId)
        set(TagSubjects.tenantId, subject.tenantId)
        set(TagSubjects.subjectType, subject.subjectType)
        set(TagSubjects.displayName, subject.displayName)
        set(TagSubjects.profileJson, subject.profileJson)
        set(TagSubjects.stateVersion, subject.stateVersion)
        set(TagSubjects.firstSeenTime, subject.firstSeenTime)
        set(TagSubjects.updateTime, subject.updateTime)
    } == 1

    /** Advances state with an optimistic guard and the full tenant-scoped subject identity. */
    open fun advanceStateVersion(
        key: TagSubjectKey,
        expectedVersion: Long,
        nextVersion: Long,
        updateTime: LocalDateTime,
    ): Boolean = database().update(TagSubjects) {
        set(TagSubjects.stateVersion, nextVersion)
        set(TagSubjects.updateTime, updateTime)
        where {
            (TagSubjects.tenantId eq key.tenantId) and
                (TagSubjects.subjectType eq key.subjectType) and
                (TagSubjects.subjectId eq key.subjectId) and
                (TagSubjects.stateVersion eq expectedVersion)
        }
    } == 1
}

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
    open fun listKeysAfter(
        tenantId: String,
        subjectType: String,
        afterSubjectId: String?,
        limit: Int,
    ): List<TagSubjectKey> {
        require(limit in 1..1000) { "Subject scan limit must be between 1 and 1000." }
        return database().useConnection { connection ->
            val sql = if (afterSubjectId == null) {
                "select subject_id from tag_subject where tenant_id = ? and subject_type = ? order by subject_id limit ?"
            } else {
                "select subject_id from tag_subject where tenant_id = ? and subject_type = ? and subject_id > ? order by subject_id limit ?"
            }
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, tenantId)
                statement.setString(2, subjectType)
                if (afterSubjectId == null) {
                    statement.setInt(3, limit)
                } else {
                    statement.setString(3, afterSubjectId)
                    statement.setInt(4, limit)
                }
                statement.executeQuery().use { rows ->
                    buildList {
                        while (rows.next()) add(TagSubjectKey(tenantId, subjectType, rows.getString(1)))
                    }
                }
            }
        }
    }

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

    /** Serializes assignment resolution for one subject using its composite-key row as the lock. */
    open fun lockAssignments(key: TagSubjectKey, updateTime: LocalDateTime): Boolean = database().update(TagSubjects) {
        set(TagSubjects.updateTime, updateTime)
        where {
            (TagSubjects.tenantId eq key.tenantId) and
                (TagSubjects.subjectType eq key.subjectType) and
                (TagSubjects.subjectId eq key.subjectId)
        }
    } == 1
}

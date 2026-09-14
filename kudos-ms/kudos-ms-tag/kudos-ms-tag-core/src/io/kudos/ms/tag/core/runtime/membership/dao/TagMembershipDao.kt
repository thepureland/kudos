package io.kudos.ms.tag.core.runtime.membership.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.membership.model.po.TagMembership
import io.kudos.ms.tag.core.runtime.membership.model.table.TagMemberships
import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.lessEq
import org.ktorm.entity.filter
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
open class TagMembershipDao : BaseCrudDao<String, TagMembership, TagMemberships>() {
    open fun list(key: TagSubjectKey): List<TagMembership> = entitySequence().filter {
        (TagMemberships.tenantId eq key.tenantId) and
            (TagMemberships.subjectType eq key.subjectType) and
            (TagMemberships.subjectId eq key.subjectId)
    }.toList()

    open fun find(
        key: TagSubjectKey,
        tagId: String,
        source: TagMembershipSource,
        sourceRef: String,
    ): TagMembership? = entitySequence().firstOrNull {
        (TagMemberships.tenantId eq key.tenantId) and
            (TagMemberships.subjectType eq key.subjectType) and
            (TagMemberships.subjectId eq key.subjectId) and
            (TagMemberships.tagId eq tagId) and
            (TagMemberships.sourceType eq source.name) and
            (TagMemberships.sourceRef eq sourceRef)
    }

    open fun listExpiredActive(now: LocalDateTime, limit: Int): List<TagMembership> {
        require(limit > 0) { "Expiry batch limit must be positive." }
        return entitySequence()
            .filter { (TagMemberships.active eq true) and (TagMemberships.effectiveUntil lessEq now) }
            .sortedBy { TagMemberships.effectiveUntil }
            .toList()
            .take(limit)
    }
}

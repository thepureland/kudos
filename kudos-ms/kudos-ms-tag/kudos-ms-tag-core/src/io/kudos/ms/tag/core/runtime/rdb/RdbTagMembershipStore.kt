package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.TagMembershipStore
import io.kudos.ms.tag.core.runtime.port.TagMembershipView
import io.kudos.ms.tag.core.runtime.membership.dao.TagMembershipDao
import io.kudos.ms.tag.core.runtime.membership.model.po.TagMembership
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

open class RdbTagMembershipStore(
    private val membershipDao: TagMembershipDao,
) : TagMembershipStore {
    override fun replace(
        key: TagSubjectKey,
        tagId: String,
        source: TagMembershipSource,
        sourceRef: String,
        active: Boolean,
        version: Long,
        ruleVersion: Long?,
        effectiveFrom: Instant?,
        effectiveUntil: Instant?,
        sourceEventId: String?,
    ): TagMembershipView {
        require(version >= 0) { "Membership version must not be negative." }
        require(effectiveUntil == null || effectiveFrom == null || effectiveUntil > effectiveFrom) {
            "Membership expiry must be later than its effective start."
        }
        val membership = membershipDao.find(key, tagId, source, sourceRef) ?: TagMembership().apply {
            id = UUID.randomUUID().toString()
            tenantId = key.tenantId
            subjectType = key.subjectType
            subjectId = key.subjectId
            this.tagId = tagId
            sourceType = source.name
            this.sourceRef = sourceRef
            membershipVersion = version
        }
        require(version >= membership.membershipVersion) { "Membership update is stale." }
        membership.active = active
        membership.ruleVersion = ruleVersion
        membership.effectiveFrom = effectiveFrom?.atOffset(ZoneOffset.UTC)?.toLocalDateTime()
        membership.effectiveUntil = effectiveUntil?.atOffset(ZoneOffset.UTC)?.toLocalDateTime()
        membership.membershipVersion = version
        membership.sourceEventId = sourceEventId
        membership.updateTime = LocalDateTime.now(ZoneOffset.UTC)
        if (membershipDao.get(membership.id) == null) membershipDao.insert(membership) else check(membershipDao.update(membership))
        return membership.toView()
    }

    override fun listActive(key: TagSubjectKey): List<TagMembershipView> {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return membershipDao.list(key)
            .filter { membership ->
                membership.active &&
                    (membership.effectiveFrom == null || membership.effectiveFrom!! <= now) &&
                    (membership.effectiveUntil == null || membership.effectiveUntil!! > now)
            }
            .map { it.toView() }
    }

    override fun nextVersion(key: TagSubjectKey): Long =
        (membershipDao.list(key).maxOfOrNull { it.membershipVersion } ?: 0) + 1

    override fun find(
        key: TagSubjectKey,
        tagId: String,
        source: TagMembershipSource,
        sourceRef: String,
    ): TagMembershipView? = membershipDao.find(key, tagId, source, sourceRef)?.toView()

    private fun TagMembership.toView() = TagMembershipView(
        id = id,
        tagId = tagId,
        source = TagMembershipSource.valueOf(sourceType),
        sourceRef = sourceRef,
        ruleVersion = ruleVersion,
        membershipVersion = membershipVersion,
        effectiveFrom = effectiveFrom?.toInstant(ZoneOffset.UTC),
        effectiveUntil = effectiveUntil?.toInstant(ZoneOffset.UTC),
    )
}

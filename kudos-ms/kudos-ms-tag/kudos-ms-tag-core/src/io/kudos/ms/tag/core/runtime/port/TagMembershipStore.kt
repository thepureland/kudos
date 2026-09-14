package io.kudos.ms.tag.core.runtime.port

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import java.time.Instant

interface TagMembershipStore {
    fun replace(
        key: TagSubjectKey,
        tagId: String,
        source: TagMembershipSource,
        sourceRef: String,
        active: Boolean,
        version: Long,
        ruleVersion: Long? = null,
        effectiveFrom: Instant? = null,
        effectiveUntil: Instant? = null,
        sourceEventId: String? = null,
    ): TagMembershipView

    fun listActive(key: TagSubjectKey): List<TagMembershipView>
    fun nextVersion(key: TagSubjectKey): Long
    fun find(key: TagSubjectKey, tagId: String, source: TagMembershipSource, sourceRef: String): TagMembershipView?
}

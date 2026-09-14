package io.kudos.ms.tag.core.runtime.port

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.subject.model.TagSubjectKey

interface TagMembershipStore {
    fun replace(
        key: TagSubjectKey,
        tagId: String,
        source: TagMembershipSource,
        sourceRef: String,
        active: Boolean,
        version: Long,
    )

    fun listActive(key: TagSubjectKey): List<TagMembershipView>
}

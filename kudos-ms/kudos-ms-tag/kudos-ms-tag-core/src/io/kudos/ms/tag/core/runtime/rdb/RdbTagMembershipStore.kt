package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.common.assignment.model.TagMembershipSource
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.runtime.port.TagMembershipStore
import io.kudos.ms.tag.core.runtime.port.TagMembershipView

/** Default RDB adapter boundary; membership resolution is implemented in Task 10. */
open class RdbTagMembershipStore : TagMembershipStore {
    override fun replace(
        key: TagSubjectKey,
        tagId: String,
        source: TagMembershipSource,
        sourceRef: String,
        active: Boolean,
        version: Long,
    ): Unit = error("RDB membership mutation is not initialized yet.")

    override fun listActive(key: TagSubjectKey): List<TagMembershipView> =
        error("RDB membership loading is not initialized yet.")
}

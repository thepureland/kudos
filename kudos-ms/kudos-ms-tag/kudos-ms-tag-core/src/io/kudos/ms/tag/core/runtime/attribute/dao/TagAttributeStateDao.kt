package io.kudos.ms.tag.core.runtime.attribute.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.attribute.model.po.TagAttributeState
import io.kudos.ms.tag.core.runtime.attribute.model.table.TagAttributeStates
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagAttributeStateDao : BaseCrudDao<String, TagAttributeState, TagAttributeStates>() {
    open fun list(key: TagSubjectKey, attributeId: String): List<TagAttributeState> =
        entitySequence().filter {
            (TagAttributeStates.tenantId eq key.tenantId) and
                (TagAttributeStates.subjectType eq key.subjectType) and
                (TagAttributeStates.subjectId eq key.subjectId) and
                (TagAttributeStates.attributeId eq attributeId)
        }.sortedBy { TagAttributeStates.valueKey }.toList()
}

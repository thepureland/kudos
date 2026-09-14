package io.kudos.ms.tag.core.catalog.tagset.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.catalog.tagset.model.po.TagSet
import io.kudos.ms.tag.core.catalog.tagset.model.table.TagSets
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagSetDao : BaseCrudDao<String, TagSet, TagSets>() {
    open fun findByCode(tenantId: String, subjectTypeCode: String, code: String): TagSet? =
        entitySequence().firstOrNull {
            (TagSets.tenantId eq tenantId) and (TagSets.subjectType eq subjectTypeCode) and (TagSets.code eq code)
        }

    open fun listBySubjectType(tenantId: String, subjectTypeCode: String): List<TagSet> =
        entitySequence().filter {
            (TagSets.tenantId eq tenantId) and (TagSets.subjectType eq subjectTypeCode)
        }.sortedBy { TagSets.code }.toList()
}

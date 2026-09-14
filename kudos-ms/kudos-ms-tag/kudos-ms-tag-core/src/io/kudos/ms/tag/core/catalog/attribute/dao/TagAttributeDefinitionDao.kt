package io.kudos.ms.tag.core.catalog.attribute.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.catalog.attribute.model.po.TagAttributeDefinition
import io.kudos.ms.tag.core.catalog.attribute.model.table.TagAttributeDefinitions
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagAttributeDefinitionDao : BaseCrudDao<String, TagAttributeDefinition, TagAttributeDefinitions>() {
    open fun findByCode(tenantId: String, subjectTypeCode: String, code: String): TagAttributeDefinition? =
        entitySequence().firstOrNull {
            (TagAttributeDefinitions.tenantId eq tenantId) and
                (TagAttributeDefinitions.subjectType eq subjectTypeCode) and
                (TagAttributeDefinitions.code eq code)
        }

    open fun listBySubjectType(tenantId: String, subjectTypeCode: String): List<TagAttributeDefinition> =
        entitySequence().filter {
            (TagAttributeDefinitions.tenantId eq tenantId) and
                (TagAttributeDefinitions.subjectType eq subjectTypeCode)
        }.sortedBy { TagAttributeDefinitions.code }.toList()
}

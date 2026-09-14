package io.kudos.ms.tag.core.catalog.tag.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.catalog.tag.model.table.TagDefinitions
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagDefinitionDao : BaseCrudDao<String, TagDefinition, TagDefinitions>() {
    open fun listByTenantAndCode(tenantId: String, code: String): List<TagDefinition> =
        entitySequence().filter {
            (TagDefinitions.tenantId eq tenantId) and (TagDefinitions.code eq code)
        }.sortedBy { TagDefinitions.subjectType }.toList()

    open fun findByCode(tenantId: String, subjectTypeCode: String, code: String): TagDefinition? =
        entitySequence().firstOrNull {
            (TagDefinitions.tenantId eq tenantId) and
                (TagDefinitions.subjectType eq subjectTypeCode) and
                (TagDefinitions.code eq code)
        }

    open fun listBySubjectType(tenantId: String, subjectTypeCode: String): List<TagDefinition> =
        entitySequence().filter {
            (TagDefinitions.tenantId eq tenantId) and (TagDefinitions.subjectType eq subjectTypeCode)
        }.sortedBy { TagDefinitions.code }.toList()

    open fun updateCatalog(entity: TagDefinition): Boolean {
        val current = requireNotNull(get(entity.id)) { "Tag definition [${entity.id}] does not exist." }
        require(current.tenantId == entity.tenantId && current.subjectType == entity.subjectType) {
            "Tag definition tenant and subject type are immutable."
        }
        require(current.code == entity.code) { "Tag definition code is immutable." }
        return update(entity)
    }
}

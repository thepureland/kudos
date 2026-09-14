package io.kudos.ms.tag.core.catalog.subjecttype.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.catalog.subjecttype.model.po.TagSubjectType
import io.kudos.ms.tag.core.catalog.subjecttype.model.table.TagSubjectTypes
import org.ktorm.dsl.eq
import org.ktorm.entity.firstOrNull
import org.springframework.stereotype.Repository

@Repository
open class TagSubjectTypeDao : BaseCrudDao<String, TagSubjectType, TagSubjectTypes>() {
    open fun findByCode(code: String): TagSubjectType? = entitySequence().firstOrNull { TagSubjectTypes.code eq code }
}

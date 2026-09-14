package io.kudos.ms.tag.core.catalog.subjecttype.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

interface TagSubjectType : IManagedDbEntity<String, TagSubjectType> {
    companion object : DbEntityFactory<TagSubjectType>()
    var code: String
    var name: String
    var ownerServiceCode: String
    var description: String?
    var version: Long
}

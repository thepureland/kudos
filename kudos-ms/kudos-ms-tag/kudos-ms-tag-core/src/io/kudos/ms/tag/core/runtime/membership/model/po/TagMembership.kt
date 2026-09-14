package io.kudos.ms.tag.core.runtime.membership.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

interface TagMembership : IDbEntity<String, TagMembership> {
    companion object : DbEntityFactory<TagMembership>()
    var tenantId: String
    var subjectType: String
    var subjectId: String
    var tagId: String
    var sourceType: String
    var sourceRef: String
    var active: Boolean
    var ruleVersion: Long?
    var effectiveFrom: LocalDateTime?
    var effectiveUntil: LocalDateTime?
    var membershipVersion: Long
    var sourceEventId: String?
    var updateTime: LocalDateTime
}

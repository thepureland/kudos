package io.kudos.ms.tag.core.rule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.model.contract.common.IAuditable
import java.time.LocalDateTime

interface TagRule : IDbEntity<String, TagRule>, IAuditable {
    companion object : DbEntityFactory<TagRule>()
    var tenantId: String
    var tagId: String
    var ruleVersion: Long
    var status: String
    var rootNodeId: String?
    var expressionVersion: Int
    var checksum: String
    var publishedTime: LocalDateTime?
    var retiredTime: LocalDateTime?
    var version: Long
}

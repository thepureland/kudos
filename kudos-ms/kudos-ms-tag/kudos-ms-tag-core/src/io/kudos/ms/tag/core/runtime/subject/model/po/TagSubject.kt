package io.kudos.ms.tag.core.runtime.subject.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

interface TagSubject : IDbEntity<String, TagSubject> {
    companion object : DbEntityFactory<TagSubject>()
    var tenantId: String
    var subjectType: String
    var subjectId: String
    var displayName: String?
    var profileJson: String?
    var stateVersion: Long
    var firstSeenTime: LocalDateTime
    var updateTime: LocalDateTime

    override var id: String
        get() = subjectId
        set(value) { subjectId = value }
}

package io.kudos.ms.sys.core.microservice.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.query.sort.Sortable
import java.time.LocalDateTime

/**
 * Micro-service DB entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysMicroService : IDbEntity<String, SysMicroService> {

    companion object : DbEntityFactory<SysMicroService>()

    /** Code */
    @get:Sortable
    var code: String

    /** Name */
    @get:Sortable
    var name: String

    /** Context */
    var context: String

    /** Whether atomic service */
    var atomicService: Boolean

    /** Parent service code */
    var parentCode: String?

    /** Remark */
    var remark: String?

    /** Whether active */
    var active: Boolean

    /** Whether built-in */
    var builtIn: Boolean?

    /** Creator id */
    var createUserId: String?

    /** Creator name */
    var createUserName: String?

    /** Create time */
    var createTime: LocalDateTime?

    /** Updater id */
    var updateUserId: String?

    /** Updater name */
    var updateUserName: String?

    /** Update time */
    var updateTime: LocalDateTime?


    override var id: String
        get() = this.code
        set(value) { this.code = value }

}

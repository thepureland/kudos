package io.kudos.ms.sys.core.system.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.query.sort.Sortable
import java.time.LocalDateTime

/**
 * System database entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysSystem : IDbEntity<String, SysSystem> {

    companion object : DbEntityFactory<SysSystem>()

    /** Code */
    @get:Sortable
    var code: String

    /** Name */
    @get:Sortable
    var name: String

    /** Whether it is a sub-system */
    var subSystem: Boolean

    /** Parent system code */
    var parentCode: String?

    /** Remark */
    var remark: String?

    /** Whether enabled */
    var active: Boolean

    /** Whether built-in */
    var builtIn: Boolean?

    /** Creator id */
    var createUserId: String?

    /** Creator name */
    var createUserName: String?

    /** Created time */
    var createTime: LocalDateTime?

    /** Updater id */
    var updateUserId: String?

    /** Updater name */
    var updateUserName: String?

    /** Updated time */
    var updateTime: LocalDateTime?



    override var id: String
        get() = this.code
        set(value) { this.code = value }

}

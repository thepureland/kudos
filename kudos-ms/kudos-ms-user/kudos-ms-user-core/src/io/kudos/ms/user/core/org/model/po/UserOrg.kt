package io.kudos.ms.user.core.org.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Organization database entity.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface UserOrg : IDbEntity<String, UserOrg> {

    companion object : DbEntityFactory<UserOrg>()

    /** Organization name. */
    var name: String

    /** Organization short name. */
    var shortName: String?

    /** Tenant id. */
    var tenantId: String

    /** Parent organization id. */
    var parentId: String?

    /** Organization type dictionary code. */
    var orgTypeDictCode: String

    /** Sort number. */
    var sortNum: Int?

    /** Remark. */
    var remark: String?

    /** Whether active. */
    var active: Boolean

    /** Whether built-in. */
    var builtIn: Boolean?

    /** Creator id. */
    var createUserId: String?

    /** Creator name. */
    var createUserName: String?

    /** Create time. */
    var createTime: LocalDateTime?

    /** Updater id. */
    var updateUserId: String?

    /** Updater name. */
    var updateUserName: String?

    /** Update time. */
    var updateTime: LocalDateTime?




}

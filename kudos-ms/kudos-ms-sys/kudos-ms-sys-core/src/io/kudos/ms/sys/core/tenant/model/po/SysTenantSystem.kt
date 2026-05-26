package io.kudos.ms.sys.core.tenant.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Tenant-system relationship database entity
 *
 * @author K
 * @since 1.0.0
 */
interface SysTenantSystem : IDbEntity<String, SysTenantSystem> {

    companion object : DbEntityFactory<SysTenantSystem>()

    /** Tenant id */
    var tenantId: String

    /** System code */
    var systemCode: String

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

}

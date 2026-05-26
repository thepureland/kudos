package io.kudos.ms.sys.core.tenant.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Tenant-locale relationship database entity
 *
 * @author K
 * @since 1.0.0
 */
interface SysTenantLocale : IDbEntity<String, SysTenantLocale> {

    companion object : DbEntityFactory<SysTenantLocale>()

    /** Tenant id */
    var tenantId: String

    /** Locale code */
    var localeCode: String

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
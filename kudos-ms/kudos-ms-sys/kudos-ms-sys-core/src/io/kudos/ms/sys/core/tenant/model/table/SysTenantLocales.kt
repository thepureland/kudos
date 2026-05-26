package io.kudos.ms.sys.core.tenant.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.tenant.model.po.SysTenantLocale
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Tenant-locale relationship table-entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
object SysTenantLocales : StringIdTable<SysTenantLocale>("sys_tenant_locale") {

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Locale code */
    var localeCode = varchar("locale_code").bindTo { it.localeCode }

    /** Creator id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Creator name */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Create time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Update time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }




}
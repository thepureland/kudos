package io.kudos.ms.user.core.account.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * Third-party account binding table-entity binding object
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object UserAccountThirds : ManagedTable<UserAccountThird>("user_account_third") {

    /** Associated user account ID */
    var userId = varchar("user_id").bindTo { it.userId }

    /** Third-party provider dict code */
    var accountProviderDictCode = varchar("account_provider_dict_code").bindTo { it.accountProviderDictCode }

    /** Issuer / provider tenant */
    var accountProviderIssuer = varchar("account_provider_issuer").bindTo { it.accountProviderIssuer }

    /** Third-party user unique identifier */
    var subject = varchar("subject").bindTo { it.subject }

    /** Cross-application unified identifier */
    var unionId = varchar("union_id").bindTo { it.unionId }

    /** Third-party display name */
    var externalDisplayName = varchar("external_display_name").bindTo { it.externalDisplayName }

    /** Third-party email */
    var externalEmail = varchar("external_email").bindTo { it.externalEmail }

    /** Avatar URL */
    var avatarUrl = varchar("avatar_url").bindTo { it.avatarUrl }

    /** Last login time */
    var lastLoginTime = datetime("last_login_time").bindTo { it.lastLoginTime }

    /** Tenant ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }




}

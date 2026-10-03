package io.kudos.ms.auth.core.organization.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Organization administrator designation (G-6, G-10). A designation, not a role.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthOrganizationAdmin : IDbEntity<String, AuthOrganizationAdmin> {

    companion object : DbEntityFactory<AuthOrganizationAdmin>()

    /** Organization */
    var organizationId: String

    /** Designated account */
    var userId: String

    /** Designated by */
    var createUserId: String?

    /** When */
    var createTime: LocalDateTime?
}

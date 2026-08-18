package io.kudos.ms.user.core.org.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

/**
 * Organization database entity.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface UserOrg : IManagedDbEntity<String, UserOrg> {

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




}

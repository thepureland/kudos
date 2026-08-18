package io.kudos.ms.auth.core.group.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

/**
 * User group database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface AuthGroup : IManagedDbEntity<String, AuthGroup> {

    companion object : DbEntityFactory<AuthGroup>()

    /** Group code. */
    var code: String

    /** Group name. */
    var name: String

    /** Tenant id. */
    var tenantId: String

    /** Subsystem code. */
    var subsysCode: String




}

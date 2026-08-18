package io.kudos.ms.auth.core.role.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

/**
 * Role database entity
 *
 * @author K
 * @author AI: Codex
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthRole : IManagedDbEntity<String, AuthRole> {

    companion object : DbEntityFactory<AuthRole>()

    /** Role code */
    var code: String

    /** Role name */
    var name: String

    /** Tenant id */
    var tenantId: String

    /** Subsystem code */
    var subsysCode: String

    /** Parent role id (NULL = root). Children inherit the parent's resource grants. */
    var parentId: String?

    /** Data-scope policy code (see DataScopeEnum); NULL = ALL (no row restriction). */
    var dataScope: String?

    /** Delegation ceiling: how many hops a grant of this role may ever carry. 0 = no delegation. */
    var delegableMax: Int?

    /** Whether assigning this role requires an approval workflow (see auth_role_grant_request). */
    var approvalRequired: Boolean?




}

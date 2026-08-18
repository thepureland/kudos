package io.kudos.ms.auth.core.role.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.auth.core.role.model.po.AuthRole
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Role database table-entity association object
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
object AuthRoles : ManagedTable<AuthRole>("auth_role") {

    /** Role code */
    var code = varchar("code").bindTo { it.code }

    /** Role name */
    var name = varchar("name").bindTo { it.name }

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Subsystem code */
    var subsysCode = varchar("subsys_code").bindTo { it.subsysCode }

    /** Parent role id (NULL = root role). */
    var parentId = varchar("parent_id").bindTo { it.parentId }

    /** Data-scope policy code (see DataScopeEnum); NULL = ALL. */
    var dataScope = varchar("data_scope").bindTo { it.dataScope }

    /** Delegation ceiling: how many hops a grant of this role may ever carry. 0 = no delegation. */
    var delegableMax = int("delegable_max").bindTo { it.delegableMax }

    /** Whether assigning this role requires approval. */
    var approvalRequired = boolean("approval_required").bindTo { it.approvalRequired }




}

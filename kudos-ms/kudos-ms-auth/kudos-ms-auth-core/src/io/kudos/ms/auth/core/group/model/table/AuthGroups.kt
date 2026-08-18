package io.kudos.ms.auth.core.group.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import org.ktorm.schema.varchar


/**
 * User group table mapping.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object AuthGroups : ManagedTable<AuthGroup>("auth_group") {

    /** Group code. */
    var code = varchar("code").bindTo { it.code }

    /** Group name. */
    var name = varchar("name").bindTo { it.name }

    /** Tenant id. */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Subsystem code. */
    var subsysCode = varchar("subsys_code").bindTo { it.subsysCode }




}

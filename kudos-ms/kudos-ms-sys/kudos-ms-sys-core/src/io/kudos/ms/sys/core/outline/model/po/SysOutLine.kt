package io.kudos.ms.sys.core.outline.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable


/**
 * Outbound allowlist DB entity.
 *
 * @author K
 * @since 1.0.0
 */
interface SysOutLine : IManagedDbEntity<String, SysOutLine> {

    companion object : DbEntityFactory<SysOutLine>()

    /** Name */
    @get:Sortable
    var name: String

    /** Host or wildcard (e.g. *.example.com) */
    var host: String

    /** Port; `null` means any port */
    var port: Int?

    /** Protocol (http/https/tcp/any) */
    var protocol: String

    /** System code */
    var systemCode: String

    /** Tenant id; `null` means platform-level */
    var tenantId: String?

}

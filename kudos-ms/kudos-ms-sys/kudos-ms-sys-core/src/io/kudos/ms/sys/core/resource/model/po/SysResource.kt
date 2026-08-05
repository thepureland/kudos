package io.kudos.ms.sys.core.resource.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Resource database entity
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface SysResource : IManagedDbEntity<String, SysResource> {

    companion object : DbEntityFactory<SysResource>()

    /** Name */
    @get:Sortable
    var name: String

    /**
     * Semantic permission code (`域:资源类型:动作`, e.g. `sys:user:delete`) — the durable identity of
     * this permission point, stable across environments and grantable with `*` wildcards. The
     * primary key is not: it differs per deployment and dies with the row.
     */
    var permissionCode: String?

    /** URL */
    var url: String?

    /** Resource type dictionary code */
    var resourceTypeDictCode: String

    /** Parent id */
    var parentId: String?

    /** Order number among siblings under the same parent */
    var orderNum: Int?

    /** Icon */
    var icon: String?

    /** Subsystem code */
    var subSystemCode: String

}
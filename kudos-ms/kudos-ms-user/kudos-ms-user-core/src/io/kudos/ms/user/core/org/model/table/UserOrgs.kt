package io.kudos.ms.user.core.org.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.user.core.org.model.po.UserOrg
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Organization database table-entity binding object.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object UserOrgs : ManagedTable<UserOrg>("user_org") {

    /** Organization name. */
    var name = varchar("name").bindTo { it.name }

    /** Organization short name. */
    var shortName = varchar("short_name").bindTo { it.shortName }

    /** Tenant id. */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Parent organization id. */
    var parentId = varchar("parent_id").bindTo { it.parentId }

    /** Organization type dictionary code. */
    var orgTypeDictCode = varchar("org_type_dict_code").bindTo { it.orgTypeDictCode }

    /** Sort number. */
    var sortNum = int("sort_num").bindTo { it.sortNum }




}

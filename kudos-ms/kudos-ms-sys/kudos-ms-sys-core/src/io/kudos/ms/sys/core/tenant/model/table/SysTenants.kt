package io.kudos.ms.sys.core.tenant.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import org.ktorm.schema.varchar


/**
 * Tenant table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysTenants : ManagedTable<SysTenant>("sys_tenant") {

    /** Name */
    var name = varchar("name").bindTo { it.name }

    /** Timezone */
    var timezone = varchar("timezone").bindTo { it.timezone }

    /** Default language code */
    var defaultLanguageCode = varchar("default_language_code").bindTo { it.defaultLanguageCode }




}
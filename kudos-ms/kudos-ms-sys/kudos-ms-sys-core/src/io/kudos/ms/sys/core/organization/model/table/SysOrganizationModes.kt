package io.kudos.ms.sys.core.organization.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.sys.core.organization.model.po.SysOrganizationMode
import org.ktorm.schema.datetime

/**
 * Organization mode marker table binding.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object SysOrganizationModes : StringIdTable<SysOrganizationMode>("sys_organization_mode") {

    /** First enable time */
    var enabledTime = datetime("enabled_time").bindTo { it.enabledTime }
}

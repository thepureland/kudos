package io.kudos.ms.sys.core.organization.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Organization mode enable marker (single row).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface SysOrganizationMode : IDbEntity<String, SysOrganizationMode> {

    companion object : DbEntityFactory<SysOrganizationMode>()

    /** When organization mode first started successfully */
    var enabledTime: LocalDateTime
}

package io.kudos.ms.sys.core.organization.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.organization.model.po.SysOrganizationMode
import io.kudos.ms.sys.core.organization.model.table.SysOrganizationModes
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Organization mode marker DAO.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class SysOrganizationModeDao : BaseCrudDao<String, SysOrganizationMode, SysOrganizationModes>() {

    /** Whether organization mode has ever started on this database. */
    open fun isMarked(): Boolean = get(MARKER_ID) != null

    /** Writes the marker; idempotent. */
    open fun mark() {
        if (isMarked()) return
        insert(SysOrganizationMode {
            id = MARKER_ID
            enabledTime = LocalDateTime.now()
        })
    }

    companion object {
        const val MARKER_ID = "organization-mode"
    }
}

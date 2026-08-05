package io.kudos.ms.auth.core.platform.authz.enforcement

import io.kudos.ability.security.enforcement.point.IPermissionPointStore
import io.kudos.ability.security.enforcement.point.PermissionPointDefinition
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.model.po.SysResource
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


/**
 * Persists declared permission points into `sys_resource`, the registry the enforcement filter reads.
 *
 * **What it does and does not touch.** A point's row is identified by `(permission_code,
 * sub_system_code)`. On first sight the row is created; on every subsequent boot only the fields
 * this module owns — `url` — are refreshed. `name`, `icon`, `order_num`, `parent_id` and `active`
 * are left exactly as found, because those are the *menu projection* of the permission and belong
 * to whoever curates the console, not to a startup scan. A registrar that rewrote them would undo
 * an operator's work on every deploy.
 *
 * **It never deletes.** A point that vanished from the code may still carry grants; removing the
 * row would turn a refactor into an invisible permission change, and would also make the
 * enforcement filter start refusing an endpoint that still exists elsewhere in the cluster during
 * a rolling deploy. Retiring a point is a deliberate administrative act.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class SysResourcePermissionPointStore : IPermissionPointStore {

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

    @Resource
    private lateinit var permissionPointRegistry: PermissionPointRegistry

    private val log = LogFactory.getLog(this::class)

    @Transactional
    override fun register(points: Collection<PermissionPointDefinition>): Int {
        if (points.isEmpty()) return 0

        // One read per subsystem rather than per point: a few hundred endpoints is the normal size
        // of this call, and a per-point existence check would be a few hundred round trips at boot.
        val bySubsystem = points.groupBy { it.subsystem }
        var affected = 0
        bySubsystem.forEach { (subsystem, subsystemPoints) ->
            val codes = subsystemPoints.map { it.code }.distinct()
            val existing = sysResourceDao.search(
                Criteria.and(
                    SysResource::subSystemCode eq subsystem,
                    Criteria(SysResource::permissionCode.name, io.kudos.base.query.enums.OperatorEnum.IN, codes),
                ),
            ).associateBy { it.permissionCode }

            val toInsert = mutableListOf<SysResource>()
            subsystemPoints.forEach { point ->
                val row = existing[point.code]
                if (row == null) {
                    toInsert += SysResource {
                        this.name = point.name
                        this.permissionCode = point.code
                        this.url = point.url
                        this.resourceTypeDictCode = RESOURCE_TYPE_FUNCTION
                        this.subSystemCode = point.subsystem
                        this.remark = point.remark
                        this.active = true
                        this.builtIn = false
                    }
                } else if (point.url != null && row.url != point.url) {
                    // The endpoint moved. Refreshing the url keeps the registry pointing at reality;
                    // the code — the thing grants are written against — is untouched by design.
                    sysResourceDao.update(SysResource {
                        this.id = row.id
                        this.url = point.url
                    })
                    affected++
                }
            }
            if (toInsert.isNotEmpty()) {
                sysResourceDao.batchInsert(toInsert)
                affected += toInsert.size
                log.info(
                    "Registered ${toInsert.size} new permission point(s) in subsystem ${subsystem}: " +
                        toInsert.take(LOG_SAMPLE).joinToString { it.permissionCode ?: "?" } +
                        if (toInsert.size > LOG_SAMPLE) ", …" else "",
                )
            }
        }

        // The registry serves URL resolution off a TTL'd snapshot; without this the endpoints just
        // registered would stay unresolvable — and therefore refused — for up to one TTL after boot.
        if (affected > 0) permissionPointRegistry.invalidate()
        return affected
    }

    companion object {
        /** `sys_dict` resource_type: 1 = menu, 2 = function. An endpoint is a function. */
        private const val RESOURCE_TYPE_FUNCTION = "2"
        private const val LOG_SAMPLE = 10
    }
}

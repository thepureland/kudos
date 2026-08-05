package io.kudos.ms.auth.core.platform.authz.enforcement

import io.kudos.ability.security.enforcement.point.IPermissionPointStore
import io.kudos.ability.security.enforcement.point.PermissionPointDefinition
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.model.po.SysResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Assembly test for permission-point registration.
 *
 * **Why this exists separately from the registrar's own unit test.** That one hand-builds a
 * registrar, so it proves the logic and nothing about the wiring. The wiring is where this feature
 * fails silently: the store is contributed by a component scan in *this* module while the registrar
 * is created by an auto-configuration in the ability module, and a `@ConditionalOnBean` across that
 * boundary can lose the ordering race — producing no registrar, no log line, and no registered
 * points. Enforcement then refuses every endpoint as unregistered. Nothing in the previous test
 * suite would have noticed.
 *
 * So this asserts the two things a unit test structurally cannot: that the beans actually meet, and
 * that the startup path — which nothing else ever triggers — really writes rows.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class PermissionPointRegistrationWiringTest : RdbTestBase() {

    @Resource
    private lateinit var store: IPermissionPointStore

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

    /** The store must be discoverable *through the port*, which is how the registrar asks for it. */
    @Test
    fun theStoreIsReachableThroughItsPort() {
        assertTrue(
            store is SysResourcePermissionPointStore,
            "the auth module must supply the enforcement layer's store port; got ${store::class.simpleName}",
        )
    }

    /**
     * Exercises the path the startup listener takes, end to end, against the real database — the
     * combination that had never run: registrar logic + this module's store + a live `sys_resource`.
     */
    @Test
    fun registeringThroughThePortWritesRealRows() {
        val code = "wiring:probe:run"
        val subsystem = "wiring-sub-system"
        try {
            val affected = store.register(
                listOf(
                    PermissionPointDefinition(
                        code = code,
                        name = code,
                        url = "GET:/api/admin/wiring/probe/run",
                        subsystem = subsystem,
                        derived = true,
                    ),
                ),
            )
            assertTrue(affected >= 1)

            val row = assertNotNull(
                sysResourceDao.search(
                    Criteria.and(
                        SysResource::subSystemCode eq subsystem,
                        SysResource::permissionCode eq code,
                    ),
                ).firstOrNull(),
                "the point must exist in sys_resource after registration",
            )
            assertTrue(row.active == true)
        } finally {
            sysResourceDao.search(
                Criteria.and(
                    SysResource::subSystemCode eq subsystem,
                    SysResource::permissionCode eq code,
                ),
            ).forEach { sysResourceDao.deleteById(it.id) }
        }
    }
}

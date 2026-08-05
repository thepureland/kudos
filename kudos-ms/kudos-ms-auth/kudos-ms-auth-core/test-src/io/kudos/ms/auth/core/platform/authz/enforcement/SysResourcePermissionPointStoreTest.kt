package io.kudos.ms.auth.core.platform.authz.enforcement

import io.kudos.ability.security.enforcement.point.PermissionPointDefinition
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.model.po.SysResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for [SysResourcePermissionPointStore].
 *
 * This runs on **every boot of every instance**, so the properties that matter are the ones about
 * repetition and about restraint: a second run must change nothing, and neither run may overwrite
 * what an operator curated in the console.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysResourcePermissionPointStoreTest : RdbTestBase() {

    @Resource
    private lateinit var store: SysResourcePermissionPointStore

    @Resource
    private lateinit var sysResourceDao: SysResourceDao

    private val subsystem = "pps-sub-system"

    private fun point(code: String, url: String?, name: String = code) =
        PermissionPointDefinition(code = code, name = name, url = url, subsystem = subsystem, derived = true)

    private fun rowOf(code: String): SysResource? = sysResourceDao.search(
        Criteria.and(
            SysResource::subSystemCode eq subsystem,
            SysResource::permissionCode eq code,
        ),
    ).firstOrNull()

    private fun cleanUp(vararg codes: String) {
        codes.forEach { code -> rowOf(code)?.let { sysResourceDao.deleteById(it.id) } }
    }

    @Test
    fun registersAPointAsAFunctionResource() {
        val code = "pps:widget:save"
        try {
            assertEquals(1, store.register(listOf(point(code, "POST:/api/admin/pps/widget/save"))))
            val row = assertNotNull(rowOf(code))
            assertEquals("POST:/api/admin/pps/widget/save", row.url)
            assertEquals("2", row.resourceTypeDictCode, "an endpoint is a function, not a menu")
            assertTrue(row.active == true)
            assertTrue(row.builtIn == false)
        } finally {
            cleanUp(code)
        }
    }

    /** Runs on every boot of every instance — the second run has to be a no-op, not a duplicate. */
    @Test
    fun registrationIsIdempotent() {
        val code = "pps:widget:idempotent"
        val points = listOf(point(code, "POST:/api/admin/pps/widget/idempotent"))
        try {
            assertEquals(1, store.register(points))
            assertEquals(0, store.register(points), "an unchanged point must not be rewritten")
            assertEquals(
                1,
                sysResourceDao.search(
                    Criteria.and(
                        SysResource::subSystemCode eq subsystem,
                        SysResource::permissionCode eq code,
                    ),
                ).size,
            )
        } finally {
            cleanUp(code)
        }
    }

    /**
     * The url is the module's to maintain; the name, icon and ordering are the console's. Rewriting
     * them here would undo an operator's curation on every deploy — which is exactly the kind of
     * silent, recurring damage that makes teams turn a feature off.
     */
    @Test
    fun refreshesTheUrlButNeverClobbersCuratedFields() {
        val code = "pps:widget:moved"
        try {
            store.register(listOf(point(code, "POST:/api/admin/pps/widget/oldPath")))
            val created = assertNotNull(rowOf(code))
            sysResourceDao.update(SysResource {
                this.id = created.id
                this.name = "Curated display name"
                this.icon = "Star"
                this.orderNum = 7
            })

            assertEquals(1, store.register(listOf(point(code, "POST:/api/admin/pps/widget/newPath"))))

            val row = assertNotNull(rowOf(code))
            assertEquals("POST:/api/admin/pps/widget/newPath", row.url, "the endpoint moved; the registry follows")
            assertEquals("Curated display name", row.name)
            assertEquals("Star", row.icon)
            assertEquals(7, row.orderNum)
        } finally {
            cleanUp(code)
        }
    }

    /**
     * A point that vanished from the code may still carry grants, and during a rolling deploy it
     * still exists on the instances not yet replaced. Deleting it would turn a refactor into an
     * invisible permission change; retiring a point stays a deliberate administrative act.
     */
    @Test
    fun neverDeletesAPointItNoLongerSees() {
        val kept = "pps:widget:retired"
        val other = "pps:widget:current"
        try {
            store.register(listOf(point(kept, "GET:/api/admin/pps/widget/retired")))
            store.register(listOf(point(other, "GET:/api/admin/pps/widget/current")))

            assertNotNull(rowOf(kept), "still present before the narrowed registration")
            store.register(listOf(point(other, "GET:/api/admin/pps/widget/current")))
            assertNotNull(rowOf(kept), "a point absent from this run must survive it")
        } finally {
            cleanUp(kept, other)
        }
    }

    @Test
    fun anEmptyRegistrationIsANoOp() {
        assertEquals(0, store.register(emptyList()))
    }
}

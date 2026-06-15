package io.kudos.ms.sys.core.platform.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for CrudLogSyncSupport top-level helpers (pure logic, no Spring container)
 *
 * Coverage:
 * - requireStringId: IIdEntity<String> payload returns id; non-IIdEntity payload throws IllegalStateException
 *   with the entity label in the message; IIdEntity with null / non-String id throws as well
 * - completeCrudUpdate: success=true -> returns true and onSuccess runs once; success=false -> returns false
 *   and onSuccess never runs; default no-op onSuccess overload
 * - completeCrudInsert: onSuccess runs once; default no-op onSuccess overload
 *
 * @author K
 * @since 1.0.0
 */
internal class CrudLogSyncSupportTest {

    private val log = LogFactory.getLog(this::class)

    // region requireStringId

    @Test
    fun requireStringId_returnsIdForStringIdEntity() {
        val form = SysTenantFormUpdate(
            id = "11111111-2222-3333-4444-555555555555",
            name = "t-name",
            subSystemCodes = emptySet(),
            timezone = null,
            defaultLanguageCode = null,
            remark = null,
        )
        assertEquals("11111111-2222-3333-4444-555555555555", requireStringId(form, "tenant"))
    }

    @Test
    fun requireStringId_throwsForNonIdEntityPayload() {
        val ex = assertFailsWith<IllegalStateException> { requireStringId(Any(), "tenant") }
        assertTrue(ex.message!!.contains("Unsupported argument type when updating tenant"))
        assertTrue(ex.message!!.contains("kotlin.Any"))
    }

    @Test
    fun requireStringId_throwsWhenIdIsNull() {
        val entity = object : IIdEntity<String?> {
            override val id: String? = null
        }
        assertFailsWith<IllegalStateException> { requireStringId(entity, "domain") }
    }

    @Test
    fun requireStringId_throwsWhenIdIsNotString() {
        val entity = object : IIdEntity<Int> {
            override val id: Int = 42
        }
        val ex = assertFailsWith<IllegalStateException> { requireStringId(entity, "param") }
        assertTrue(ex.message!!.contains("param"))
    }

    // endregion

    // region completeCrudUpdate

    @Test
    fun completeCrudUpdate_successRunsOnSuccessAndReturnsTrue() {
        var calls = 0
        val result = completeCrudUpdate(
            success = true,
            log = log,
            successMessage = "ok",
            failureMessage = "fail",
        ) { calls++ }
        assertTrue(result)
        assertEquals(1, calls)
    }

    @Test
    fun completeCrudUpdate_failureSkipsOnSuccessAndReturnsFalse() {
        var calls = 0
        val result = completeCrudUpdate(
            success = false,
            log = log,
            successMessage = "ok",
            failureMessage = "fail",
        ) { calls++ }
        assertFalse(result)
        assertEquals(0, calls)
    }

    @Test
    fun completeCrudUpdate_defaultOnSuccessOverload() {
        assertTrue(completeCrudUpdate(success = true, log = log, successMessage = "ok", failureMessage = "fail"))
        assertFalse(completeCrudUpdate(success = false, log = log, successMessage = "ok", failureMessage = "fail"))
    }

    // endregion

    // region completeCrudInsert

    @Test
    fun completeCrudInsert_runsOnSuccessOnce() {
        var calls = 0
        completeCrudInsert(log, "inserted") { calls++ }
        assertEquals(1, calls)
    }

    @Test
    fun completeCrudInsert_defaultOnSuccessOverload() {
        // Must simply not throw
        completeCrudInsert(log, "inserted")
    }

    // endregion
}

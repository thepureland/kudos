package io.kudos.ability.log.audit.mongo.service

import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.mongo.entity.SysAuditLogDocument
import io.kudos.ability.log.audit.mongo.repository.SysAuditLogRepository
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit tests for [MongoAuditService] with a mocked [SysAuditLogRepository] — the
 * counterpart to the testcontainer-backed `MongoAuditServiceTest`, focused on paths a real
 * Mongo can't exercise deterministically:
 * - persistence failure (repository throws) → `false`, never propagates ("audit must not
 *   interrupt business flow" contract).
 * - null `entities` list and empty `entities` list → no-op success, repository untouched.
 * - null elements inside `sysAuditDetailLogs` and details with null `auditId` are dropped
 *   instead of crashing the keyed-by-auditId map build.
 * - one document is built per entity, preserving entity order.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MongoAuditServiceUnitTest {

    private val repository: SysAuditLogRepository = mock(SysAuditLogRepository::class.java)
    private val service = MongoAuditService(repository)

    @Test
    fun submit_nullEntities_isNoOpSuccess_repositoryUntouched() {
        assertTrue(service.submit(SysAuditLogModel()), "null entities list is a no-op success")
        verifyNoInteractions(repository)
    }

    @Test
    fun submit_emptyEntitiesList_isNoOpSuccess_repositoryUntouched() {
        val model = SysAuditLogModel().apply { entities = mutableListOf() }
        assertTrue(service.submit(model), "empty entities list is a no-op success")
        verifyNoInteractions(repository)
    }

    @Test
    fun submit_repositoryThrows_returnsFalseAndNeverPropagates() {
        doThrow(RuntimeException("mongo down")).`when`(repository).insert(anyIterable<SysAuditLogDocument>())
        val model = SysAuditLogModel().apply {
            tenantId = "T"
            subSysCode = "SYS"
            entities = mutableListOf(newEntity("a-1"))
        }

        val result = service.submit(model) // must not throw

        assertFalse(result, "persistence failure surfaces as false, not as an exception")
    }

    @Test
    fun submit_nullDetailElementAndNullAuditIdDetail_areDroppedSilently() {
        val model = SysAuditLogModel().apply {
            entities = mutableListOf(newEntity("a-1"))
            sysAuditDetailLogs = mutableListOf(
                null, // null element must be filtered, not NPE
                SysAuditDetailLogVo().apply { id = "orphan"; auditId = null; operateUrl = "/orphan" },
                SysAuditDetailLogVo().apply { id = "d-1"; auditId = "a-1"; operateUrl = "/real" },
            )
        }

        assertTrue(service.submit(model))

        val docs = capturedInsertedDocs()
        assertEquals(1, docs.size)
        val detail = assertNotNull(docs.single().detail, "the keyed detail must still attach")
        assertEquals("/real", detail.operateUrl)
        assertEquals("d-1", detail.id)
    }

    @Test
    fun submit_buildsOneDocumentPerEntity_preservingOrderAndFallbacks() {
        val model = SysAuditLogModel().apply {
            tenantId = "T-model"
            subSysCode = "SYS-model"
            entities = mutableListOf(
                newEntity("first").apply { tenantId = "T-own" },
                newEntity("second"),
                newEntity("third"),
            )
            sysAuditDetailLogs = mutableListOf(
                SysAuditDetailLogVo().apply { auditId = "second"; operateUrl = "/2" },
            )
        }

        assertTrue(service.submit(model))

        val docs = capturedInsertedDocs()
        assertEquals(listOf("first", "second", "third"), docs.map { it.id }, "entity order is preserved")
        assertEquals("T-own", docs[0].tenantId, "entity's own tenant wins")
        assertEquals("T-model", docs[1].tenantId, "missing entity tenant falls back to model")
        assertEquals("SYS-model", docs[2].subSysCode, "missing entity subSysCode falls back to model")
        assertNull(docs[0].detail)
        assertEquals("/2", docs[1].detail?.operateUrl)
        assertNull(docs[2].detail)
    }

    @Suppress("UNCHECKED_CAST")
    private fun capturedInsertedDocs(): List<SysAuditLogDocument> {
        val captor = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<SysAuditLogDocument>>
        verify(repository).insert(captor.capture())
        return captor.value.toList()
    }

    private fun newEntity(id: String): SysAuditLogVo = SysAuditLogVo().also { it.id = id }
}

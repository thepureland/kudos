package io.kudos.ability.log.audit.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.mongo.entity.SysAuditLogDocument
import io.kudos.ability.log.audit.mongo.repository.SysAuditLogRepository
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MongoTestContainer
import jakarta.annotation.Resource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for [MongoAuditService] against a real Mongo 7 testcontainer.
 *
 * Coverage:
 *  - Empty model returns true without touching Mongo (no-op success — audit on a no-op operation
 *    isn't a failure).
 *  - A populated model writes one document per entity, each carrying its matching detail
 *    embedded as a sub-document under `detail`.
 *  - tenantId / subSysCode use the kudos fallback rule: entity's own value first, fall back to
 *    the model-level value when the entity didn't carry one.
 *  - An entity with no matching detail in the model still inserts cleanly with `detail = null`.
 *  - Top-level repository / MongoTemplate beans are autowired correctly (proves the
 *    `@EnableMongoRepositories(basePackageClasses)` scoping works without component-scanning the
 *    host app).
 *
 * Test-side `@TestConfiguration MongoClient` works around the same Spring Boot 4 race documented
 * in `kudos-ability-data-docdb-mongo`: the auto-config wires the MongoClient before
 * `@DynamicPropertySource` lands. Real apps aren't affected because their application.yml is
 * applied before context refresh.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@EnabledIfDockerInstalled
@Import(MongoAuditServiceTest.TestMongoClientConfig::class)
internal class MongoAuditServiceTest {

    @Resource
    private lateinit var service: IAuditService

    @Resource
    private lateinit var repository: SysAuditLogRepository

    @Resource
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeTest
    fun cleanUp() {
        mongoTemplate.dropCollection(SysAuditLogDocument::class.java)
    }

    @AfterTest
    fun cleanUpAfter() {
        mongoTemplate.dropCollection(SysAuditLogDocument::class.java)
    }

    @Test
    fun submit_emptyModel_isNoOpSuccess() {
        val empty = SysAuditLogModel()
        assertTrue(service.submit(empty), "empty model is a no-op success — audit on nothing isn't a failure")
        assertEquals(0L, repository.count(), "no document was written")
    }

    @Test
    fun submit_writesOneDocumentPerEntity_withEmbeddedDetail() {
        val now = Date()
        val model = newModel(modelTenant = "T-default", modelSubSys = "SYS-default").apply {
            entities = mutableListOf(
                newEntity(id = "audit-1", entityId = "user-101", desc = "create user", operateTime = now),
                newEntity(id = "audit-2", entityId = "user-102", desc = "delete user", operateTime = now),
            )
            sysAuditDetailLogs = mutableListOf(
                newDetail(auditId = "audit-1", operateUrl = "/api/users", description = "create user"),
                newDetail(auditId = "audit-2", operateUrl = "/api/users/102", description = "delete user"),
            )
        }

        assertTrue(service.submit(model))

        assertEquals(2L, repository.count())
        val first = assertNotNull(repository.findById("audit-1").orElse(null))
        assertEquals("user-101", first.entityId)
        assertEquals("create user", first.description)
        assertEquals(now, first.operateTime)
        val firstDetail = assertNotNull(first.detail, "detail must be embedded, not absent")
        assertEquals("/api/users", firstDetail.operateUrl)
        assertEquals("create user", firstDetail.description)
    }

    @Test
    fun submit_tenantAndSubSys_fallBackToModelLevelWhenEntityFieldsAbsent() {
        val model = newModel(modelTenant = "T-fallback", modelSubSys = "SYS-fallback").apply {
            entities = mutableListOf(
                // entity carries its own tenant + subSysCode → must be preserved
                newEntity(id = "with-own", tenantId = "T-own", subSysCode = "SYS-own"),
                // entity leaves both null → must fall back to model-level
                newEntity(id = "with-fallback"),
            )
        }

        assertTrue(service.submit(model))

        val owned = assertNotNull(repository.findById("with-own").orElse(null))
        assertEquals("T-own", owned.tenantId, "entity's own tenantId wins")
        assertEquals("SYS-own", owned.subSysCode, "entity's own subSysCode wins")

        val fellBack = assertNotNull(repository.findById("with-fallback").orElse(null))
        assertEquals("T-fallback", fellBack.tenantId, "missing entity tenantId falls back to model.tenantId")
        assertEquals("SYS-fallback", fellBack.subSysCode, "missing entity subSysCode falls back to model.subSysCode")
    }

    @Test
    fun submit_entityWithoutMatchingDetail_stillInserts_detailIsNull() {
        val model = newModel().apply {
            entities = mutableListOf(newEntity(id = "no-detail"))
            sysAuditDetailLogs = mutableListOf() // empty — no detail at all
        }

        assertTrue(service.submit(model))

        val doc = assertNotNull(repository.findById("no-detail").orElse(null))
        assertNull(doc.detail, "entity with no matching detail is allowed; detail field stays null")
    }

    @Test
    fun submit_detailsKeyedByAuditId_matchedCorrectly() {
        // Mixed scenario: 3 entities + 2 details. Only entity-A and entity-C have matching
        // details. Entity-B should land with detail=null even though the detail list isn't empty.
        val model = newModel().apply {
            entities = mutableListOf(
                newEntity(id = "entity-A"),
                newEntity(id = "entity-B"),
                newEntity(id = "entity-C"),
            )
            sysAuditDetailLogs = mutableListOf(
                newDetail(auditId = "entity-A", operateUrl = "/A"),
                newDetail(auditId = "entity-C", operateUrl = "/C"),
            )
        }

        assertTrue(service.submit(model))

        assertEquals("/A", repository.findById("entity-A").orElse(null)?.detail?.operateUrl)
        assertNull(repository.findById("entity-B").orElse(null)?.detail, "no detail for B → embedded field stays null")
        assertEquals("/C", repository.findById("entity-C").orElse(null)?.detail?.operateUrl)
    }

    @Test
    fun submit_detailWithoutMatchingEntity_isSilentlyDropped() {
        // A stray detail whose auditId doesn't match any entity must NOT create a phantom document
        // or crash the call. Audit pipelines shouldn't be brittle to inconsistent upstream data.
        val model = newModel().apply {
            entities = mutableListOf(newEntity(id = "real-entity"))
            sysAuditDetailLogs = mutableListOf(
                newDetail(auditId = "real-entity", operateUrl = "/real"),
                newDetail(auditId = "ghost-entity", operateUrl = "/ghost"),
            )
        }

        assertTrue(service.submit(model))

        assertEquals(1L, repository.count(), "only the real entity creates a document; the ghost detail is dropped")
        assertEquals("/real", repository.findById("real-entity").orElse(null)?.detail?.operateUrl)
    }

    private fun newModel(
        modelTenant: String? = null,
        modelSubSys: String? = null,
    ): SysAuditLogModel = SysAuditLogModel().apply {
        tenantId = modelTenant
        subSysCode = modelSubSys
    }

    private fun newEntity(
        id: String,
        entityId: String? = null,
        desc: String? = null,
        operateTime: Date? = null,
        tenantId: String? = null,
        subSysCode: String? = null,
    ): SysAuditLogVo = SysAuditLogVo().also {
        it.id = id
        it.entityId = entityId
        it.description = desc
        it.operateTime = operateTime
        it.tenantId = tenantId
        it.subSysCode = subSysCode
    }

    private fun newDetail(
        auditId: String,
        operateUrl: String? = null,
        description: String? = null,
    ): SysAuditDetailLogVo = SysAuditDetailLogVo().also {
        it.id = "detail-$auditId"
        it.auditId = auditId
        it.operateUrl = operateUrl
        it.description = description
    }

    @TestConfiguration(proxyBeanMethods = false)
    open class TestMongoClientConfig {
        @Bean
        @Primary
        open fun testMongoClient(environment: Environment): MongoClient {
            val uri = requireNotNull(environment.getProperty("spring.data.mongodb.uri")) {
                "spring.data.mongodb.uri was not registered; MongoTestContainer.startIfNeeded() should have set it"
            }
            return MongoClients.create(uri)
        }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            MongoTestContainer.startIfNeeded(registry)
        }
    }
}

package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.table.TestAutoIncKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestAutoIncKtormDao
import io.kudos.ability.data.rdb.ktorm.table.TestManagedKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestManagedKtormDao
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the audit auto-fill paths of [BaseCrudDao] against an [IManagedDbEntity] table
 * (test_managed_ktorm), plus the auto-increment-id insert path (test_auto_inc_ktorm):
 *  - insert(entity): setInsertDefault(e) fills createTime/updateTime on an auditable entity
 *  - insert without explicit id: the insertExclude(id) branch returns the DB-generated key
 *  - update(entity): setDefault(e) fills updateTime without touching create fields
 *  - updateProperties(id, map): setDefault(map) injects updateTime into the property map
 *  - batchInsert(plain payloads): setInsertDefault(map) injects audit keys into the bean-extract map
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
internal open class BaseCrudDaoAuditFillTest {

    @Resource
    private lateinit var managedDao: TestManagedKtormDao

    @Resource
    private lateinit var autoIncDao: TestAutoIncKtormDao

    @Test
    @Transactional
    open fun insert_auditableEntity_autoFillsAuditColumns() {
        val entity = TestManagedKtorm {
            id = "m-ins-1"
            name = "inserted"
        }
        val id = managedDao.insert(entity)
        assertEquals("m-ins-1", id)

        val saved = managedDao.get("m-ins-1")!!
        assertNotNull(saved.createTime, "createTime must be auto-filled on insert of an IAuditable entity")
        assertNotNull(saved.updateTime, "updateTime must be auto-filled on insert too (first version of the row)")
        assertNull(saved.createUserId, "no request user in test context -> stays null, never fabricated")
    }

    @Test
    @Transactional
    open fun insert_withoutExplicitId_usesDbGeneratedKey() {
        // TestAutoIncKtorm id is never set -> Entity.properties has no id key -> insertExclude path
        val entity = TestAutoIncKtorm { name = "auto-row" }
        val id = autoIncDao.insert(entity)
        assertTrue(id > 0, "DB-generated identity key must be returned, got $id")
        assertEquals(id, entity.id, "the generated key must be written back onto the entity")
        assertEquals("auto-row", autoIncDao.get(id)!!.name)
    }

    @Test
    @Transactional
    open fun update_auditableEntity_autoFillsUpdateFieldsOnly() {
        val entity = managedDao.get("m-1")!!
        assertNull(entity.updateTime, "fixture row must start with update_time NULL (see data.sql)")
        entity.name = "renamed"

        assertTrue(managedDao.update(entity))

        val saved = managedDao.get("m-1")!!
        assertEquals("renamed", saved.name)
        assertNotNull(saved.updateTime, "setDefault(e) must fill updateTime on update of an IAuditable entity")
        assertEquals(java.time.LocalDateTime.of(2026, 1, 1, 0, 0), saved.createTime,
            "createTime must never be modified by the update auto-fill")
    }

    @Test
    @Transactional
    open fun updateProperties_auditableTable_injectsUpdateTimeIntoMap() {
        assertTrue(managedDao.updateProperties("m-1", mapOf(TestManagedKtorm::name.name to "via-map")))

        val saved = managedDao.get("m-1")!!
        assertEquals("via-map", saved.name)
        assertNotNull(saved.updateTime, "setDefault(map) must add updateTime when the entity class is IAuditable")
    }

    @Test
    @Transactional
    open fun batchInsert_plainPayloads_onAuditableTable_injectsAuditKeys() {
        /** DTO-style payload (not an entity) to force the plain-bean batch insert path. */
        class Payload {
            var id: String? = null
            var name: String? = null
        }

        val payloads = listOf(
            Payload().apply { id = "m-bi-1"; name = "batch1" },
            Payload().apply { id = "m-bi-2"; name = "batch2" },
        )
        assertEquals(2, managedDao.batchInsert(payloads))

        val row = managedDao.get("m-bi-1")!!
        assertEquals("batch1", row.name)
        assertNotNull(row.createTime, "setInsertDefault(map) must inject createTime for DTO payloads")
        assertNotNull(row.updateTime)
    }

    @Test
    @Transactional
    open fun update_plainPayload_copiesIntoEntityAndUpdates() {
        /** DTO-style payload to force the non-entity update branch of BaseCrudDao.update. */
        class Payload {
            var id: String? = null
            var name: String? = null
        }

        val ok = managedDao.update(Payload().apply { id = "m-1"; name = "payload-renamed" })
        assertTrue(ok, "update(payload) must copy properties into a new entity and update by id")
        assertEquals("payload-renamed", managedDao.get("m-1")!!.name)
    }
}

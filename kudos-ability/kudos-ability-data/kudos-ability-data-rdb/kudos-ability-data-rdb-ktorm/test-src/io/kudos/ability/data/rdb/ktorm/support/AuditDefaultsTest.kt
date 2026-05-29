package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.model.contract.common.IAuditable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [AuditDefaults]. Cover the "fills when null, never overrides" invariant on both
 * insert and update paths, for both the IAuditable-object and Map<String, Any?> overloads.
 *
 * Uses a plain mutable POKO ([Subject]) instead of a Ktorm entity proxy — the rules only touch the
 * `IAuditable` contract, so the test fixture stays trivial.
 */
internal class AuditDefaultsTest {

    private val now = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
    private val laterNow = LocalDateTime.of(2026, 1, 2, 3, 4, 6)

    @Test
    fun fillForInsert_fillsAllAuditFieldsWhenNull() {
        val subject = Subject()

        AuditDefaults.fillForInsert(subject, now = now, userId = "u-1")

        assertEquals(now, subject.createTime)
        assertEquals("u-1", subject.createUserId)
        assertEquals(now, subject.updateTime)
        assertEquals("u-1", subject.updateUserId)
    }

    @Test
    fun fillForInsert_neverOverwritesCallerProvidedValues() {
        val subject = Subject(
            createTime = LocalDateTime.of(2025, 1, 1, 0, 0),
            createUserId = "explicit-creator",
            updateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
            updateUserId = "explicit-updater",
        )

        AuditDefaults.fillForInsert(subject, now = now, userId = "u-2")

        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), subject.createTime, "createTime must be preserved verbatim")
        assertEquals("explicit-creator", subject.createUserId)
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), subject.updateTime)
        assertEquals("explicit-updater", subject.updateUserId)
    }

    @Test
    fun fillForInsert_acceptsNullUserId_whenNoRequestContext() {
        // Scheduled-task / startup-script callers have no KudosContextHolder; auto-fill should leave
        // the user id as null rather than fabricate something like "system".
        val subject = Subject()

        AuditDefaults.fillForInsert(subject, now = now, userId = null)

        assertEquals(now, subject.createTime, "createTime is still filled even when userId is absent")
        assertNull(subject.createUserId, "missing userId stays null — caller decides on a fallback if needed")
        assertNull(subject.updateUserId)
    }

    @Test
    fun fillForUpdate_onlyTouchesUpdateFields() {
        val subject = Subject()

        AuditDefaults.fillForUpdate(subject, now = laterNow, userId = "u-3")

        assertNull(subject.createTime, "createTime must NOT be filled on update — that would erase the original creation timestamp")
        assertNull(subject.createUserId)
        assertEquals(laterNow, subject.updateTime)
        assertEquals("u-3", subject.updateUserId)
    }

    @Test
    fun fillForUpdate_neverOverwritesCallerProvidedUpdateValues() {
        val subject = Subject(
            updateTime = LocalDateTime.of(2025, 12, 31, 23, 59),
            updateUserId = "explicit-updater",
        )

        AuditDefaults.fillForUpdate(subject, now = laterNow, userId = "u-4")

        assertEquals(LocalDateTime.of(2025, 12, 31, 23, 59), subject.updateTime)
        assertEquals("explicit-updater", subject.updateUserId)
    }

    @Test
    fun fillForInsert_mapVariant_populatesAllAuditKeys() {
        val map = mutableMapOf<String, Any?>("name" to "alice")

        AuditDefaults.fillForInsert(map, now = now, userId = "u-5")

        assertEquals("alice", map["name"])
        assertEquals(now, map[IAuditable::createTime.name])
        assertEquals("u-5", map[IAuditable::createUserId.name])
        assertEquals(now, map[IAuditable::updateTime.name])
        assertEquals("u-5", map[IAuditable::updateUserId.name])
    }

    @Test
    fun fillForInsert_mapVariant_keepsExistingEntries() {
        // Key present in the map but mapped to null is still "set by caller" and must be honored.
        val map = mutableMapOf<String, Any?>(
            IAuditable::createTime.name to null,
            IAuditable::createUserId.name to "preset",
        )

        AuditDefaults.fillForInsert(map, now = now, userId = "u-6")

        assertNull(map[IAuditable::createTime.name], "Map key present even with null value means caller intent — keep it")
        assertEquals("preset", map[IAuditable::createUserId.name])
        // The other two were missing so they get filled in.
        assertEquals(now, map[IAuditable::updateTime.name])
        assertEquals("u-6", map[IAuditable::updateUserId.name])
    }

    @Test
    fun fillForUpdate_mapVariant_onlyTouchesUpdateKeys() {
        val map = mutableMapOf<String, Any?>("status" to "active")

        AuditDefaults.fillForUpdate(map, now = laterNow, userId = "u-7")

        assertEquals("active", map["status"])
        assertEquals(false, map.containsKey(IAuditable::createTime.name), "create keys never appear on update")
        assertEquals(false, map.containsKey(IAuditable::createUserId.name))
        assertEquals(laterNow, map[IAuditable::updateTime.name])
        assertEquals("u-7", map[IAuditable::updateUserId.name])
    }

    /** Minimal IAuditable fixture; nullable defaults so individual tests can pre-set selected fields. */
    private class Subject(
        override var createTime: LocalDateTime? = null,
        override var createUserId: String? = null,
        override var createUserName: String? = null,
        override var updateTime: LocalDateTime? = null,
        override var updateUserId: String? = null,
        override var updateUserName: String? = null,
    ) : IAuditable
}

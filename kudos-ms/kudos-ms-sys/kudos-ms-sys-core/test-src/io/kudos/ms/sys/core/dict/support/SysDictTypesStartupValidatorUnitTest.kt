package io.kudos.ms.sys.core.dict.support

import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import io.kudos.ms.sys.core.dict.dao.SysDictDao
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [SysDictTypesStartupValidator] (DAO mocked with Mockito, no Spring container).
 *
 * Covers the [SysDictTypesStartupValidator.onApplicationReady] branch matrix that the integration test
 * (which only calls [validate]) does not reach:
 * - validation disabled -> early return, lastResult stays null
 * - missing types + failOnMissing=false -> logs only, lastResult populated
 * - missing types + failOnMissing=true -> throws IllegalStateException
 * - extras present -> warn branch
 * - fully consistent -> success branch
 * plus the [validate] result shaping (declared / missing / extras / isOk).
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictTypesStartupValidatorUnitTest {

    private fun entry(dictType: String) = SysDictCacheEntry(
        id = "id-$dictType",
        dictType = dictType,
        dictName = "name-$dictType",
        atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME,
        remark = null,
        active = true,
        builtIn = false,
    )

    /** All declared dict types present in DB -> ok, no missing/extras. */
    private fun allDeclaredEntries(): List<SysDictCacheEntry> {
        val declared = SysDictTypes::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .mapNotNull { it.isAccessible = true; (it.get(null) as? String)?.takeIf { v -> v.isNotBlank() } }
            .toSet()
        return declared.map { entry(it) }
    }

    private fun validator(
        dao: SysDictDao,
        enabled: Boolean = true,
        failOnMissing: Boolean = false,
    ) = SysDictTypesStartupValidator(dao, enabled, failOnMissing)

    @Test
    fun validate_detectsMissingAndExtras() {
        val dao = mock(SysDictDao::class.java)
        // DB has only one declared type plus an undeclared "legacy_only"
        whenCalled(dao.searchDictsByAtomicServiceCode(SysConsts.ATOMIC_SERVICE_NAME))
            .thenReturn(listOf(entry(SysDictTypes.DS_USE), entry("legacy_only")))

        val result = validator(dao).validate()
        assertEquals(11, result.declared.size)
        assertContains(result.missing, SysDictTypes.IP_TYPE)
        assertFalse(result.missing.contains(SysDictTypes.DS_USE))
        assertContains(result.extras, "legacy_only")
        assertFalse(result.isOk)
        // sorted ascending
        assertEquals(result.missing.sorted(), result.missing)
        assertEquals(result.extras.sorted(), result.extras)
    }

    @Test
    fun validate_okWhenAllPresent() {
        val dao = mock(SysDictDao::class.java)
        whenCalled(dao.searchDictsByAtomicServiceCode(SysConsts.ATOMIC_SERVICE_NAME))
            .thenReturn(allDeclaredEntries())
        val result = validator(dao).validate()
        assertTrue(result.missing.isEmpty())
        assertTrue(result.extras.isEmpty())
        assertTrue(result.isOk)
    }

    @Test
    fun onApplicationReady_disabled_returnsEarly() {
        val dao = mock(SysDictDao::class.java)
        val v = validator(dao, enabled = false)
        v.onApplicationReady()
        assertNull(v.lastResult, "disabled validation must not populate lastResult")
    }

    @Test
    fun onApplicationReady_missingWithoutFail_logsAndStoresResult() {
        val dao = mock(SysDictDao::class.java)
        whenCalled(dao.searchDictsByAtomicServiceCode(SysConsts.ATOMIC_SERVICE_NAME))
            .thenReturn(emptyList()) // everything missing
        val v = validator(dao, enabled = true, failOnMissing = false)
        v.onApplicationReady()
        val result = assertNotNull(v.lastResult)
        assertTrue(result.missing.isNotEmpty())
        assertFalse(result.isOk)
    }

    @Test
    fun onApplicationReady_missingWithFail_throws() {
        val dao = mock(SysDictDao::class.java)
        whenCalled(dao.searchDictsByAtomicServiceCode(SysConsts.ATOMIC_SERVICE_NAME))
            .thenReturn(emptyList())
        val v = validator(dao, enabled = true, failOnMissing = true)
        assertFailsWith<IllegalStateException> { v.onApplicationReady() }
    }

    @Test
    fun onApplicationReady_extrasOnly_warnsAndOk() {
        val dao = mock(SysDictDao::class.java)
        // all declared present + an extra undeclared type -> missing empty, extras non-empty
        whenCalled(dao.searchDictsByAtomicServiceCode(SysConsts.ATOMIC_SERVICE_NAME))
            .thenReturn(allDeclaredEntries() + entry("extra_legacy"))
        val v = validator(dao)
        v.onApplicationReady()
        val result = assertNotNull(v.lastResult)
        assertTrue(result.missing.isEmpty())
        assertContains(result.extras, "extra_legacy")
        assertTrue(result.isOk)
    }

    @Test
    fun onApplicationReady_consistent_successBranch() {
        val dao = mock(SysDictDao::class.java)
        whenCalled(dao.searchDictsByAtomicServiceCode(SysConsts.ATOMIC_SERVICE_NAME))
            .thenReturn(allDeclaredEntries())
        val v = validator(dao)
        v.onApplicationReady()
        val result = assertNotNull(v.lastResult)
        assertTrue(result.isOk)
        assertTrue(result.missing.isEmpty() && result.extras.isEmpty())
    }
}

package io.kudos.ms.sys.common.locale.api

import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for ISysLocaleApi
 *
 * Covers getLocaleByCode (found / not-found) and listActiveLocales (ordered list)
 * via a fake implementation.
 *
 * @author K
 * @since 1.0.0
 */
internal class ISysLocaleApiTest {

    private fun cacheEntry(code: String, sortNo: Int) = SysLocaleCacheEntry(
        id = "l-$code", code = code, displayName = code, englishName = code,
        sortNo = sortNo, remark = null, active = true, builtIn = false,
    )

    private inner class FakeApi : ISysLocaleApi {
        var lastCode: String? = null

        override fun getLocaleByCode(code: String): SysLocaleCacheEntry? {
            lastCode = code
            return if (code == "zh_CN") cacheEntry("zh_CN", 0) else null
        }

        override fun listActiveLocales(): List<SysLocaleCacheEntry> =
            listOf(cacheEntry("zh_CN", 0), cacheEntry("en_US", 1))
    }

    @Test
    fun getLocaleByCodeFound() {
        val api = FakeApi()
        val entry = api.getLocaleByCode("zh_CN")
        assertEquals("zh_CN", api.lastCode)
        assertEquals("zh_CN", entry?.code)
    }

    @Test
    fun getLocaleByCodeNotFound() {
        val api = FakeApi()
        assertNull(api.getLocaleByCode("xx_XX"))
    }

    @Test
    fun listActiveLocales() {
        val api = FakeApi()
        val list = api.listActiveLocales()
        assertEquals(2, list.size)
        assertEquals("zh_CN", list[0].code)
        assertTrue(list[0].sortNo <= list[1].sortNo)
    }

}

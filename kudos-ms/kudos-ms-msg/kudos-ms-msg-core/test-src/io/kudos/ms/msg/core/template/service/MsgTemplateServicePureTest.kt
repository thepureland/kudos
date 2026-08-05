package io.kudos.ms.msg.core.template.service

import io.kudos.base.query.Criteria
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.support.MockitoKt.anyNN
import io.kudos.ms.msg.core.support.MockitoKt.eqNN
import io.kudos.ms.msg.core.template.dao.MsgTemplateDao
import io.kudos.ms.msg.core.template.service.impl.MsgTemplateService
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.mockito.Mockito.`when` as whenCalled

/**
 * Pure unit test for [MsgTemplateService] — DAO mocked, no Spring container / DB.
 *
 * Covered:
 * - getTemplateById: delegates to `dao.getAs<MsgTemplateCacheEntry>(id)` (inlines to `get(id, KClass)`)
 * - getTemplateByEvent: delegates to `dao.searchAs<MsgTemplateCacheEntry>(criteria)` (inlines to
 *   `search(criteria, KClass)`) and returns the first row, or null when the search is empty. Both the
 *   locale-supplied and locale-null branches reach the same search call.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgTemplateServicePureTest {

    private val dao = mock(MsgTemplateDao::class.java)
    private val service = MsgTemplateService(dao)

    private fun entry(id: String) = MsgTemplateCacheEntry(
        id = id,
        sendTypeDictCode = "01",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        receiverGroupCode = null,
        localeDictCode = "zh_CN",
        title = "t",
        content = "c",
        defaultActive = true,
        defaultTitle = "dt",
        defaultContent = "dc",
        tenantId = "t1",
    )

    @Test
    fun getTemplateById_returnsEntry() {
        val e = entry("tmpl-1")
        whenCalled(dao.get(anyString(), eqNN(MsgTemplateCacheEntry::class))).thenReturn(e)
        assertEquals(e, service.getTemplateById("tmpl-1"))
    }

    @Test
    fun getTemplateById_returnsNullWhenNotFound() {
        whenCalled(dao.get(anyString(), eqNN(MsgTemplateCacheEntry::class))).thenReturn(null)
        assertNull(service.getTemplateById("missing"))
    }

    @Test
    fun getTemplateByEvent_returnsFirstMatchWithLocale() {
        val first = entry("found")
        whenCalled(dao.search(anyNN(Criteria::class.java), eqNN(MsgTemplateCacheEntry::class)))
            .thenReturn(listOf(first, entry("other")))

        val result = service.getTemplateByEvent(
            tenantId = "t1",
            eventTypeDictCode = "user_registered",
            msgTypeDictCode = "welcome",
            localeDictCode = "zh_CN",
        )
        assertEquals("found", result?.id)
    }

    @Test
    fun getTemplateByEvent_returnsFirstMatchWithNullLocale() {
        val first = entry("found")
        whenCalled(dao.search(anyNN(Criteria::class.java), eqNN(MsgTemplateCacheEntry::class)))
            .thenReturn(listOf(first))

        val result = service.getTemplateByEvent(
            tenantId = "t1",
            eventTypeDictCode = "user_registered",
            msgTypeDictCode = "welcome",
            localeDictCode = null,
        )
        assertEquals("found", result?.id)
    }

    @Test
    fun getTemplateByEvent_returnsNullWhenEmpty() {
        whenCalled(dao.search(anyNN(Criteria::class.java), eqNN(MsgTemplateCacheEntry::class)))
            .thenReturn(emptyList())

        val result = service.getTemplateByEvent(
            tenantId = "t1",
            eventTypeDictCode = "no_such",
            msgTypeDictCode = "welcome",
            localeDictCode = "zh_CN",
        )
        assertNull(result)
    }
}

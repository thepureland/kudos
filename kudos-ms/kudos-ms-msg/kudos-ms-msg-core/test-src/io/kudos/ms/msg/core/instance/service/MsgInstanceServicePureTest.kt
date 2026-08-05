package io.kudos.ms.msg.core.instance.service

import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import io.kudos.ms.msg.core.instance.dao.MsgInstanceDao
import io.kudos.ms.msg.core.instance.service.impl.MsgInstanceService
import io.kudos.ms.msg.core.support.MockitoKt.eqNN
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for [MsgInstanceService.getInstanceById] — DAO mocked, no Spring container / DB.
 *
 * `getInstanceById` delegates to `dao.getAs<MsgInstanceCacheEntry>(id)`, which inlines to
 * `dao.get(id, MsgInstanceCacheEntry::class)`; both the found and not-found paths are covered.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgInstanceServicePureTest {

    private val dao = mock(MsgInstanceDao::class.java)
    private val service = MsgInstanceService(dao)

    private fun entry(id: String) = MsgInstanceCacheEntry(
        id = id,
        localeDictCode = "zh_CN",
        title = "t",
        content = "c",
        templateId = "tmpl-1",
        sendTypeDictCode = "01",
        eventTypeDictCode = "user_registered",
        msgTypeDictCode = "welcome",
        validTimeStart = null,
        validTimeEnd = null,
        tenantId = "t1",
    )

    @Test
    fun getInstanceById_returnsEntry() {
        val e = entry("i-1")
        whenCalled(dao.get(anyString(), eqNN(MsgInstanceCacheEntry::class))).thenReturn(e)
        assertEquals(e, service.getInstanceById("i-1"))
    }

    @Test
    fun getInstanceById_returnsNullWhenNotFound() {
        whenCalled(dao.get(anyString(), eqNN(MsgInstanceCacheEntry::class))).thenReturn(null)
        assertNull(service.getInstanceById("missing"))
    }
}

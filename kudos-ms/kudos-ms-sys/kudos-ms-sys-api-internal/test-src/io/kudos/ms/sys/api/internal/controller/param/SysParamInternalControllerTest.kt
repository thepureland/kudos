package io.kudos.ms.sys.api.internal.controller.param

import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.core.param.api.SysParamApi
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * test for SysParamInternalController (pure delegation, core api mocked with Mockito, no Spring container)
 *
 * Coverage:
 * - getParam delegates and returns the same entry on a hit
 * - getParam returns null on a miss (delegation of null)
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamInternalControllerTest {

    private val api = mock(SysParamApi::class.java)
    private val controller = SysParamInternalController(api)

    @Test
    fun getParam_delegates_hit() {
        val entry = mock(SysParamCacheEntry::class.java)
        whenCalled(api.getParam("p.name", "atomic-a")).thenReturn(entry)

        assertSame(entry, controller.getParam("p.name", "atomic-a"))
        verify(api).getParam("p.name", "atomic-a")
    }

    @Test
    fun getParam_delegates_miss() {
        whenCalled(api.getParam("p.missing", "atomic-a")).thenReturn(null)

        assertNull(controller.getParam("p.missing", "atomic-a"))
        verify(api).getParam("p.missing", "atomic-a")
    }
}

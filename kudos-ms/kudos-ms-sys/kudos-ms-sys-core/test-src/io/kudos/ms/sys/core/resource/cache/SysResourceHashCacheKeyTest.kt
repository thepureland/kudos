package io.kudos.ms.sys.core.resource.cache

import io.kudos.context.support.Consts
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for the composite-key builders of [SysResourceHashCache]
 * ([getKeySubSysAndUrl] / [getKeySubSysAndType]). These are plain string-formatting helpers
 * that do not touch the DAO, so a bare instance (lateinit deps left unset) suffices.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysResourceHashCacheKeyTest {

    private val cache = SysResourceHashCache()
    private val delimiter = Consts.CACHE_KEY_DEFAULT_DELIMITER

    @Test
    fun getKeySubSysAndUrl_normal() {
        assertEquals("sub-a${delimiter}/path/x", cache.getKeySubSysAndUrl("sub-a", "/path/x"))
    }

    @Test
    fun getKeySubSysAndUrl_nullUrl_rendersLiteralNull() {
        // current contract: a null url is rendered through string templating as the literal "null"
        assertEquals("sub-a${delimiter}null", cache.getKeySubSysAndUrl("sub-a", null))
    }

    @Test
    fun getKeySubSysAndUrl_emptyUrl() {
        assertEquals("sub-a${delimiter}", cache.getKeySubSysAndUrl("sub-a", ""))
    }

    @Test
    fun getKeySubSysAndType_normal() {
        assertEquals("sub-b${delimiter}menu", cache.getKeySubSysAndType("sub-b", "menu"))
    }

    @Test
    fun getKeySubSysAndType_unicode() {
        assertEquals("子系统${delimiter}菜单", cache.getKeySubSysAndType("子系统", "菜单"))
    }
}

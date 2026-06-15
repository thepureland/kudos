package io.kudos.ms.sys.core.cache.api

import io.kudos.ms.sys.common.cache.api.ISysCacheApi
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Pure unit test for [SysCacheApi]: it is the local marker implementation of [ISysCacheApi] with no behavior.
 * Verifies it can be instantiated and is usable through the [ISysCacheApi] contract type.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysCacheApiTest {

    @Test
    fun instantiates_andUsableAsApiType() {
        val api: ISysCacheApi = SysCacheApi()
        assertNotNull(api)
    }
}

package io.kudos.ms.sys.core.dict.api

import io.kudos.ms.sys.common.dict.api.ISysDictItemApi
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * test for SysDictItemApi.
 *
 * SysDictItemApi is a marker implementation of [ISysDictItemApi] with no business methods of its own; this
 * test simply verifies it can be instantiated and conforms to the interface contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemApiTest {

    @Test
    fun instantiation_conformsToInterface() {
        val api: ISysDictItemApi = SysDictItemApi()
        assertNotNull(api)
    }
}

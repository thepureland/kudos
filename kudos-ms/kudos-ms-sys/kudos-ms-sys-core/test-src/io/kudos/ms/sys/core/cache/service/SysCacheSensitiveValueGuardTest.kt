package io.kudos.ms.sys.core.cache.service

import io.kudos.ms.sys.core.cache.service.impl.SysCacheService
import io.kudos.ms.sys.core.datasource.cache.SysDataSourceHashCache
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit test for the sensitive-cache export blacklist used by `SysCacheService.getValueJson`;
 * no Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysCacheSensitiveValueGuardTest {

    @Test
    fun dataSourceCacheIsSensitive() {
        assertTrue(SysCacheService.isSensitiveValueCache(SysDataSourceHashCache.CACHE_NAME))
    }

    @Test
    fun ordinaryCachesAreExportable() {
        assertFalse(SysCacheService.isSensitiveValueCache("SYS_DICT_ITEM__HASH"))
        assertFalse(SysCacheService.isSensitiveValueCache(""))
        // matching is exact, not prefix-based
        assertFalse(SysCacheService.isSensitiveValueCache(SysDataSourceHashCache.CACHE_NAME.lowercase()))
    }
}

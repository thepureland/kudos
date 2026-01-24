package io.kudos.ams.sys.provider.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantLanguageDao
 *
 * 测试数据来源：`SysTenantLanguageDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantLanguageDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysTenantLanguageDao: SysTenantLanguageDao

    @Test
    fun searchLanguageCodesByTenantId() {
        val tenantId = "40000000-0000-0000-0000-000000000090"
        val codes = sysTenantLanguageDao.searchLanguageCodesByTenantId(tenantId)
        assertTrue(codes.size >= 2)
        assertTrue(codes.contains("zh-CN"))
        assertTrue(codes.contains("en-US"))
    }

    @Test
    fun searchTenantIdsByLanguageCode() {
        val languageCode = "zh-CN"
        val tenantIds = sysTenantLanguageDao.searchTenantIdsByLanguageCode(languageCode)
        assertTrue(tenantIds.size >= 2)
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000000090"))
        assertTrue(tenantIds.contains("40000000-0000-0000-0000-000000000091"))
    }

    @Test
    fun exists() {
        // 测试存在的关系
        assertTrue(sysTenantLanguageDao.exists("40000000-0000-0000-0000-000000000090", "zh-CN"))
        
        // 测试不存在的关系
        assertFalse(sysTenantLanguageDao.exists("40000000-0000-0000-0000-000000000090", "non-existent-lang"))
    }
}

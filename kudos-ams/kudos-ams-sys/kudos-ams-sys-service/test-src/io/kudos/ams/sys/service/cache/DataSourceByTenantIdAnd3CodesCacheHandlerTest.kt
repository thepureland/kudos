package io.kudos.ams.sys.service.cache

import io.kudos.ams.sys.service.dao.SysDataSourceDao
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test

/**
 * junit test for DataSourceByTenantIdAnd3CodesCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
class DataSourceByTenantIdAnd3CodesCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: DataSourceByTenantIdAnd3CodesCacheHandler

    @Autowired
    private lateinit var sysDataSourceDao: SysDataSourceDao

    @Test
    fun reloadAll() {
    }

    @Test
    fun getDataSource() {
        val dsId = "33333333-e828-43c5-a512-111111111111"
//        cacheHandler.getDataSource()
    }

    @Test
    fun syncOnInsert() {
    }

    @Test
    fun syncOnUpdate() {
    }

    @Test
    fun syncOnUpdateActive() {
    }

    @Test
    fun syncOnDelete() {
    }

}
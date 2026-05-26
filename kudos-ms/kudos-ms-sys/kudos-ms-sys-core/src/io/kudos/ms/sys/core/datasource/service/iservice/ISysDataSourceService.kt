package io.kudos.ms.sys.core.datasource.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceRow
import io.kudos.ms.sys.core.datasource.model.po.SysDataSource


/**
 * Data source service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDataSourceService : IBaseCrudService<String, SysDataSource> {

    /**
     * Load a data source cache entry by primary key id and cache the result.
     *
     * @param id data source primary key, non-blank
     * @return cache entry, or null if not found
     */
    fun getDataSourceFromCache(id: String): SysDataSourceCacheEntry?

    /**
     * Query the data source list from cache by tenant id, sub-system code and micro-service code (includes inactive).
     */
    fun getDataSourcesFromCache(
        tenantId: String,
        subSystemCode: String,
        microServiceCode: String?
    ): List<SysDataSourceCacheEntry>

    /**
     * Fetch a single data source from cache by tenant id and atomic service code
     * (internally queries by tenantId + subSystem=null + microService=atomicServiceCode and takes the first match).
     */
    fun getDataSourceFromCache(tenantId: String, atomicServiceCode: String?): SysDataSourceCacheEntry?

    /**
     * Update the enabled state and sync the cache.
     *
     * @param id primary key
     * @param active whether enabled
     * @return whether the update succeeded
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * Reset password.
     *
     * @param id primary key
     * @param newPassword new password
     */
    fun resetPassword(id: String, newPassword: String)

    /**
     * Get the data source list for a tenant.
     *
     * @param tenantId tenant id
     * @return list of data source records
     */
    fun getDataSourcesByTenantId(tenantId: String): List<SysDataSourceRow>

    /**
     * Get the data source list for a sub-system.
     *
     * @param subSystemCode sub-system code
     * @return list of data source records
     */
    fun getDataSourcesBySubSystemCode(subSystemCode: String): List<SysDataSourceRow>

    /**
     * Test JDBC connectivity: opens a temporary connection with the given url/username/password and runs a ping statement.
     * The connection is closed before this method returns; no state is persisted.
     *
     * @param url JDBC URL
     * @param username username
     * @param password password; may be null (some drivers allow it)
     * @return true if connect + ping succeeded; false if any step failed (exceptions are swallowed and converted to false)
     */
    fun testConnection(url: String, username: String, password: String?): Boolean


}

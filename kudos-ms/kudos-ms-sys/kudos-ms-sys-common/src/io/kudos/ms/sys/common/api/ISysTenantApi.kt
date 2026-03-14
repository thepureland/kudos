package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.tenant.SysTenantCacheEntry


/**
 * 租户 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantApi {


    /**
     * 返回指定id的租户(包括未启用的)
     *
     * @param id 租户id
     * @return 租户信息对象，包括未启用的。找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getTenant(id: String): SysTenantCacheEntry?

    /**
     * 根据id集合取得对应租户信息，并将结果缓存，查不到不缓存
     *
     * @param ids 租户id集合
     * @return Map(租户id, SysTenantCacheEntry)
     * @author K
     * @since 1.0.0
     */
    fun getTenantsBySubSystemCode(ids: Collection<String>): Map<String, SysTenantCacheEntry>

    /**
     * 返回指定子系统的所有租户(仅启用的)
     *
     * @param subSystemCode 子系统代码
     * @return List(租户信息对象)
     * @author K
     * @since 1.0.0
     */
    fun getTenantsBySubSystemCode(subSystemCode: String): List<SysTenantCacheEntry>


}
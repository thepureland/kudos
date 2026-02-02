package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.core.model.po.SysTenantLocale


/**
 * 租户-语言关系业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantLocaleService : IBaseCrudService<String, SysTenantLocale> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id获取语言代码列表
     *
     * @param tenantId 租户id
     * @return 语言代码集合
     * @author K
     * @since 1.0.0
     */
    fun getLocaleCodesByTenantId(tenantId: String): Set<String>

    /**
     * 根据语言代码获取租户id列表
     *
     * @param localeCode 语言代码
     * @return 租户id集合
     * @author K
     * @since 1.0.0
     */
    fun getTenantIdsByLocaleCode(localeCode: String): Set<String>

    /**
     * 批量绑定租户与语言的关系
     *
     * @param tenantId 租户id
     * @param localeCodes 语言代码集合
     * @return 成功绑定的数量
     * @author K
     * @since 1.0.0
     */
    fun batchBind(tenantId: String, localeCodes: Collection<String>): Int

    /**
     * 解绑租户与语言的关系
     *
     * @param tenantId 租户id
     * @param localeCode 语言代码
     * @return 是否解绑成功
     * @author K
     * @since 1.0.0
     */
    fun unbind(tenantId: String, localeCode: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param localeCode 语言代码
     * @return 是否存在
     * @author K
     * @since 1.0.0
     */
    fun exists(tenantId: String, localeCode: String): Boolean

    //endregion your codes 2

}
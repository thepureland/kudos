package io.kudos.ams.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.core.model.po.SysTenantLanguage


/**
 * 租户-语言关系业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantLanguageService : IBaseCrudService<String, SysTenantLanguage> {
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
    fun getLanguageCodesByTenantId(tenantId: String): Set<String>

    /**
     * 根据语言代码获取租户id列表
     *
     * @param languageCode 语言代码
     * @return 租户id集合
     * @author K
     * @since 1.0.0
     */
    fun getTenantIdsByLanguageCode(languageCode: String): Set<String>

    /**
     * 批量绑定租户与语言的关系
     *
     * @param tenantId 租户id
     * @param languageCodes 语言代码集合
     * @return 成功绑定的数量
     * @author K
     * @since 1.0.0
     */
    fun batchBind(tenantId: String, languageCodes: Collection<String>): Int

    /**
     * 解绑租户与语言的关系
     *
     * @param tenantId 租户id
     * @param languageCode 语言代码
     * @return 是否解绑成功
     * @author K
     * @since 1.0.0
     */
    fun unbind(tenantId: String, languageCode: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param languageCode 语言代码
     * @return 是否存在
     * @author K
     * @since 1.0.0
     */
    fun exists(tenantId: String, languageCode: String): Boolean

    //endregion your codes 2

}
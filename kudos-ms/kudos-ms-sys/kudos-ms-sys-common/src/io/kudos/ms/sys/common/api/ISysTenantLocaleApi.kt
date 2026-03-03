package io.kudos.ms.sys.common.api


/**
 * 租户-语言关系 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantLocaleApi {
//endregion your codes 1

    //region your codes 2

    fun getLocaleCodesByTenantId(tenantId: String): Set<String>

    fun getTenantIdsByLocaleCode(localeCode: String): Set<String>

    fun batchBind(tenantId: String, localeCodes: Collection<String>): Int

    fun unbind(tenantId: String, localeCode: String): Boolean

    fun exists(tenantId: String, localeCode: String): Boolean

    //endregion your codes 2

}
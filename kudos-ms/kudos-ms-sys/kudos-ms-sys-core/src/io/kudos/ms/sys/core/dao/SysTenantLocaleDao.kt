package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.model.po.SysTenantLocale
import io.kudos.ms.sys.core.model.table.SysTenantLocales
import org.springframework.stereotype.Repository


/**
 * 租户-语言关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysTenantLocaleDao : BaseCrudDao<String, SysTenantLocale, SysTenantLocales>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id查找对应的语言代码
     *
     * @param tenantId 租户id
     * @return Set<语言代码>
     */
    fun searchLocaleCodesByTenantId(tenantId: String): Set<String> {
        val criteria = Criteria(SysTenantLocale::tenantId eq tenantId)
        return searchProperty(criteria, SysTenantLocale::localeCode).toSet()
    }

    /**
     * 根据语言代码查找对应的租户id
     *
     * @param localeCode 语言代码
     * @return Set<租户id>
     */
    fun searchTenantIdsByLocaleCode(localeCode: String): Set<String> {
        val criteria = Criteria(SysTenantLocale::localeCode eq localeCode)
        return searchProperty(criteria, SysTenantLocale::tenantId).toSet()
    }

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param localeCode 语言代码
     * @return 是否存在
     */
    fun exists(tenantId: String, localeCode: String): Boolean {
        val criteria = Criteria.and(
            SysTenantLocale::tenantId eq tenantId,
            SysTenantLocale::localeCode eq localeCode
        )
        return count(criteria) > 0
    }

    /**
     * 按租户ID和语言代码删除关系
     *
     * @param tenantId 租户ID
     * @param localeCode 语言代码
     * @return 删除条数
     */
    fun deleteByTenantIdAndLocaleCode(tenantId: String, localeCode: String): Int {
        val criteria = Criteria.and(
            SysTenantLocale::tenantId eq tenantId,
            SysTenantLocale::localeCode eq localeCode
        )
        return batchDeleteCriteria(criteria)
    }

    //endregion your codes 2

}
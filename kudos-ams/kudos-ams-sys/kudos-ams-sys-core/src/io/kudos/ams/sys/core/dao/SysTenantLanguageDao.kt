package io.kudos.ams.sys.core.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ams.sys.core.model.po.SysTenantLanguage
import io.kudos.ams.sys.core.model.table.SysTenantLanguages
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 租户-语言关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysTenantLanguageDao : BaseCrudDao<String, SysTenantLanguage, SysTenantLanguages>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id查找对应的语言代码
     *
     * @param tenantId 租户id
     * @return Set<语言代码>
     */
    fun searchLanguageCodesByTenantId(tenantId: String): Set<String> {
        val criteria = Criteria.of(SysTenantLanguage::tenantId.name, OperatorEnum.EQ, tenantId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysTenantLanguage::languageCode.name).toSet() as Set<String>
    }

    /**
     * 根据语言代码查找对应的租户id
     *
     * @param languageCode 语言代码
     * @return Set<租户id>
     */
    fun searchTenantIdsByLanguageCode(languageCode: String): Set<String> {
        val criteria = Criteria.of(SysTenantLanguage::languageCode.name, OperatorEnum.EQ, languageCode)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysTenantLanguage::tenantId.name).toSet() as Set<String>
    }

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param languageCode 语言代码
     * @return 是否存在
     * @author AI: Cursor
     */
    fun exists(tenantId: String, languageCode: String): Boolean {
        val criteria = Criteria.of(SysTenantLanguage::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(SysTenantLanguage::languageCode.name, OperatorEnum.EQ, languageCode)
        return count(criteria) > 0
    }

    //endregion your codes 2

}
package io.kudos.ms.sys.core.service.impl

import io.kudos.ms.sys.core.service.iservice.ISysTenantLanguageService
import io.kudos.ms.sys.core.model.po.SysTenantLanguage
import io.kudos.ms.sys.core.dao.SysTenantLanguageDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.logger.LogFactory
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 租户-语言关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantLanguageService : BaseCrudService<String, SysTenantLanguage, SysTenantLanguageDao>(), ISysTenantLanguageService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getLanguageCodesByTenantId(tenantId: String): Set<String> {
        return dao.searchLanguageCodesByTenantId(tenantId)
    }

    override fun getTenantIdsByLanguageCode(languageCode: String): Set<String> {
        return dao.searchTenantIdsByLanguageCode(languageCode)
    }

    @Transactional
    override fun batchBind(tenantId: String, languageCodes: Collection<String>): Int {
        if (languageCodes.isEmpty()) {
            return 0
        }
        var count = 0
        languageCodes.forEach { languageCode ->
            if (!exists(tenantId, languageCode)) {
                val relation = SysTenantLanguage {
                    this.tenantId = tenantId
                    this.languageCode = languageCode
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定租户${tenantId}与${languageCodes.size}种语言的关系，成功绑定${count}条。")
        return count
    }

    @Transactional
    override fun unbind(tenantId: String, languageCode: String): Boolean {
        val criteria = Criteria.of(SysTenantLanguage::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(SysTenantLanguage::languageCode.name, OperatorEnum.EQ, languageCode)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑租户${tenantId}与语言${languageCode}的关系。")
        } else {
            log.warn("解绑租户${tenantId}与语言${languageCode}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(tenantId: String, languageCode: String): Boolean {
        return dao.exists(tenantId, languageCode)
    }

    //endregion your codes 2

}
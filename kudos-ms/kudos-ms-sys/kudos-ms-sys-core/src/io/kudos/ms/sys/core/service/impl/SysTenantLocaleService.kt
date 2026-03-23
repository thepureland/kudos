package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.dao.SysTenantLocaleDao
import io.kudos.ms.sys.core.model.po.SysTenantLocale
import io.kudos.ms.sys.core.service.iservice.ISysTenantLocaleService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 租户-语言关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysTenantLocaleService(
    dao: SysTenantLocaleDao
) : BaseCrudService<String, SysTenantLocale, SysTenantLocaleDao>(dao), ISysTenantLocaleService {


    private val log = LogFactory.getLog(this)

    override fun getLocaleCodesByTenantId(tenantId: String): Set<String> {
        return dao.searchLocaleCodesByTenantId(tenantId)
    }

    override fun getTenantIdsByLocaleCode(localeCode: String): Set<String> {
        return dao.searchTenantIdsByLocaleCode(localeCode)
    }

    @Transactional
    override fun batchBind(tenantId: String, localeCodes: Collection<String>): Int {
        if (localeCodes.isEmpty()) {
            return 0
        }
        var count = 0
        localeCodes.forEach { localeCode ->
            if (!exists(tenantId, localeCode)) {
                val relation = SysTenantLocale {
                    this.tenantId = tenantId
                    this.localeCode = localeCode
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定租户${tenantId}与${localeCodes.size}种语言的关系，成功绑定${count}条。")
        return count
    }

    @Transactional
    override fun unbind(tenantId: String, localeCode: String): Boolean {
        val count = dao.deleteByTenantIdAndLocaleCode(tenantId, localeCode)
        val success = count > 0
        if (success) {
            log.debug("解绑租户${tenantId}与语言${localeCode}的关系。")
        } else {
            log.warn("解绑租户${tenantId}与语言${localeCode}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(tenantId: String, localeCode: String): Boolean {
        return dao.exists(tenantId, localeCode)
    }


}
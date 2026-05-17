package io.kudos.ms.sys.core.tenant.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.tenant.dao.SysTenantLocaleDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenantLocale
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantLocaleService
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

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getLocaleCodesByTenantId(tenantId: String): Set<String> = dao.searchLocaleCodesByTenantId(tenantId)

    @Transactional(readOnly = true)
    override fun getTenantIdsByLocaleCode(localeCode: String): Set<String> = dao.searchTenantIdsByLocaleCode(localeCode)

    @Transactional
    override fun batchBind(tenantId: String, localeCodes: Collection<String>): Int {
        if (localeCodes.isEmpty()) return 0

        // 一次 SELECT 已存在的关系，差集对新增 ID 一次 batchInsert，把原 N+1 折叠到 2 次 SQL。
        val existing = dao.searchLocaleCodesByTenantId(tenantId)
        val newLocaleCodes = localeCodes.toSet() - existing
        if (newLocaleCodes.isEmpty()) {
            log.debug("批量绑定租户${tenantId}与${localeCodes.size}种语言的关系，全部已存在，无新增。")
            return 0
        }
        val relations = newLocaleCodes.map {
            SysTenantLocale {
                this.tenantId = tenantId
                this.localeCode = it
            }
        }
        dao.batchInsert(relations)
        log.debug("批量绑定租户${tenantId}与${localeCodes.size}种语言的关系，成功绑定${newLocaleCodes.size}条。")
        return newLocaleCodes.size
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

    @Transactional(readOnly = true)
    override fun exists(tenantId: String, localeCode: String): Boolean = dao.exists(tenantId, localeCode)
}

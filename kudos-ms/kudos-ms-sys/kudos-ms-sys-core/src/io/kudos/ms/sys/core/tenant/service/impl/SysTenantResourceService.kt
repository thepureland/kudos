package io.kudos.ms.sys.core.tenant.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.tenant.dao.SysTenantResourceDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenantResource
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantResourceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 租户-资源关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysTenantResourceService(
    dao: SysTenantResourceDao
) : BaseCrudService<String, SysTenantResource, SysTenantResourceDao>(dao), ISysTenantResourceService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getResourceIdsByTenantId(tenantId: String): Set<String> = dao.searchResourceIdsByTenantId(tenantId)

    @Transactional(readOnly = true)
    override fun getTenantIdsByResourceId(resourceId: String): Set<String> = dao.searchTenantIdsByResourceId(resourceId)

    @Transactional
    override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int {
        if (resourceIds.isEmpty()) return 0

        // 一次 SELECT 把已存在的关系拿全，差集只对新增 ID 做一次 batchInsert，把原 N+1 (1 exists + 1 insert per id)
        // 折叠到 2 次 SQL。
        val existing = dao.searchResourceIdsByTenantId(tenantId)
        val newResourceIds = resourceIds.toSet() - existing
        if (newResourceIds.isEmpty()) {
            log.debug("批量绑定租户${tenantId}与${resourceIds.size}个资源的关系，全部已存在，无新增。")
            return 0
        }
        val relations = newResourceIds.map {
            SysTenantResource {
                this.tenantId = tenantId
                this.resourceId = it
            }
        }
        dao.batchInsert(relations)
        log.debug("批量绑定租户${tenantId}与${resourceIds.size}个资源的关系，成功绑定${newResourceIds.size}条。")
        return newResourceIds.size
    }

    @Transactional
    override fun unbind(tenantId: String, resourceId: String): Boolean {
        val count = dao.deleteByTenantIdAndResourceId(tenantId, resourceId)
        val success = count > 0
        if (success) {
            log.debug("解绑租户${tenantId}与资源${resourceId}的关系。")
        } else {
            log.warn("解绑租户${tenantId}与资源${resourceId}的关系失败，关系不存在。")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(tenantId: String, resourceId: String): Boolean = dao.exists(tenantId, resourceId)
}

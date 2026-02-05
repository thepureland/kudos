package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.core.dao.SysTenantResourceDao
import io.kudos.ms.sys.core.model.po.SysTenantResource
import io.kudos.ms.sys.core.service.iservice.ISysTenantResourceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 租户-资源关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantResourceService : BaseCrudService<String, SysTenantResource, SysTenantResourceDao>(), ISysTenantResourceService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getResourceIdsByTenantId(tenantId: String): Set<String> {
        return dao.searchResourceIdsByTenantId(tenantId)
    }

    override fun getTenantIdsByResourceId(resourceId: String): Set<String> {
        return dao.searchTenantIdsByResourceId(resourceId)
    }

    @Transactional
    override fun batchBind(tenantId: String, resourceIds: Collection<String>): Int {
        if (resourceIds.isEmpty()) {
            return 0
        }
        var count = 0
        resourceIds.forEach { resourceId ->
            if (!exists(tenantId, resourceId)) {
                val relation = SysTenantResource {
                    this.tenantId = tenantId
                    this.resourceId = resourceId
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定租户${tenantId}与${resourceIds.size}个资源的关系，成功绑定${count}条。")
        return count
    }

    @Transactional
    override fun unbind(tenantId: String, resourceId: String): Boolean {
        val criteria = Criteria.of(SysTenantResource::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(SysTenantResource::resourceId.name, OperatorEnum.EQ, resourceId)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑租户${tenantId}与资源${resourceId}的关系。")
        } else {
            log.warn("解绑租户${tenantId}与资源${resourceId}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(tenantId: String, resourceId: String): Boolean {
        return dao.exists(tenantId, resourceId)
    }

    //endregion your codes 2

}
package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysTenantSubSystemBiz
import io.kudos.ams.sys.service.model.po.SysTenantSubSystem
import io.kudos.ams.sys.service.dao.SysTenantSubSystemDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 租户-子系统关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantSubSystemBiz : BaseCrudBiz<String, SysTenantSubSystem, SysTenantSubSystemDao>(), ISysTenantSubSystemBiz {
//endregion your codes 1

    //region your codes 2

    override fun searchSubSystemCodesByTenantId(tenantId: String): Set<String> =
        dao.searchSubSystemCodesByTenantId(tenantId)

    override fun searchTenantIdsBySubSystemCode(subSystemCode: String): Set<String> =
        dao.searchTenantIdsBySubSystemCode(subSystemCode)

    override fun groupingSubSystemCodesByTenantIds(tenantIds: Collection<String>?): Map<String, List<String>> =
        dao.groupingSubSystemCodesByTenantIds(tenantIds)

    override fun groupingTenantIdsBySubSystemCodes(subSystemCodes: Collection<String>?): Map<String, List<String>> =
        dao.groupingTenantIdsBySubSystemCodes(subSystemCodes)

    //endregion your codes 2

}
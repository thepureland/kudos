package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseReadOnlyService
import io.kudos.ms.sys.common.vo.accessrule.response.VSysAccessRuleWithIpRow
import io.kudos.ms.sys.core.dao.VSysAccessRuleWithIpDao
import io.kudos.ms.sys.core.model.po.VSysAccessRuleWithIp
import io.kudos.ms.sys.core.service.iservice.IVSysAccessRuleIpService
import org.springframework.stereotype.Service

/**
 * 视图 `v_sys_access_rule_with_ip` 的只读服务实现，封装 [VSysAccessRuleWithIpDao] 的查询。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
open class VSysAccessRuleIpService(
    dao: VSysAccessRuleWithIpDao,
) : BaseReadOnlyService<String, VSysAccessRuleWithIp, VSysAccessRuleWithIpDao>(dao),
    IVSysAccessRuleIpService {

    /** 委托 [VSysAccessRuleWithIpDao.searchByParentId]。 */
    override fun searchByParentId(parentId: String): List<VSysAccessRuleWithIpRow> =
        dao.searchByParentId(parentId)

    /** 委托 [VSysAccessRuleWithIpDao.searchBySystemCodeAndTenantId]。 */
    override fun searchBySystemCodeAndTenantId(
        systemCode: String,
        tenantId: String?,
    ): List<VSysAccessRuleWithIpRow> = dao.searchBySystemCodeAndTenantId(systemCode, tenantId)
}

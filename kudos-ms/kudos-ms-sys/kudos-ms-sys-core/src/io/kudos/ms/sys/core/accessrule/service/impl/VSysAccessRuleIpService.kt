package io.kudos.ms.sys.core.accessrule.service.impl

import io.kudos.base.support.service.impl.BaseReadOnlyService
import io.kudos.ms.sys.common.accessrule.vo.response.VSysAccessRuleWithIpRow
import io.kudos.ms.sys.core.accessrule.dao.VSysAccessRuleWithIpDao
import io.kudos.ms.sys.core.accessrule.model.po.VSysAccessRuleWithIp
import io.kudos.ms.sys.core.accessrule.service.iservice.IVSysAccessRuleIpService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Read-only service implementation for the view `v_sys_access_rule_with_ip`, wrapping queries of [VSysAccessRuleWithIpDao].
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
open class VSysAccessRuleIpService(
    dao: VSysAccessRuleWithIpDao,
) : BaseReadOnlyService<String, VSysAccessRuleWithIp, VSysAccessRuleWithIpDao>(dao),
    IVSysAccessRuleIpService {

    /** Delegates to [VSysAccessRuleWithIpDao.searchByParentId]. */
    override fun searchByParentId(parentId: String): List<VSysAccessRuleWithIpRow> =
        dao.searchByParentId(parentId)

    /** Delegates to [VSysAccessRuleWithIpDao.searchBySystemCodeAndTenantId]; a blank tenantId is treated as null (platform level). */
    override fun searchBySystemCodeAndTenantId(
        systemCode: String,
        tenantId: String?,
    ): List<VSysAccessRuleWithIpRow> =
        dao.searchBySystemCodeAndTenantId(systemCode, tenantId?.takeIf { it.isNotBlank() })
}

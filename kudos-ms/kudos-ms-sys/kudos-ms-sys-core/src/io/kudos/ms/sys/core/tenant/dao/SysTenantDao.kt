package io.kudos.ms.sys.core.tenant.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.ms.sys.core.tenant.model.table.SysTenants
import org.springframework.stereotype.Repository


/**
 * 租户数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysTenantDao : BaseCrudDao<String, SysTenant, SysTenants>() {



}
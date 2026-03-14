package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.model.po.SysTenant
import io.kudos.ms.sys.core.model.table.SysTenants
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
package io.kudos.ms.sys.core.dao

import io.kudos.ms.sys.core.model.po.SysTenant
import io.kudos.ms.sys.core.model.table.SysTenants
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 租户数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysTenantDao : BaseCrudDao<String, SysTenant, SysTenants>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
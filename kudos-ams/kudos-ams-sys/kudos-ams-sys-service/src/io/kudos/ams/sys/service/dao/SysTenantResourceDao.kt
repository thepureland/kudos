package io.kudos.ams.sys.service.dao

import io.kudos.ams.sys.service.model.po.SysTenantResource
import io.kudos.ams.sys.service.model.table.SysTenantResources
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 租户-资源关系数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysTenantResourceDao : BaseCrudDao<String, SysTenantResource, SysTenantResources>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
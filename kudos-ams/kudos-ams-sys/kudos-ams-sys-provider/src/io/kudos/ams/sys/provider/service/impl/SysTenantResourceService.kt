package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysTenantResourceService
import io.kudos.ams.sys.provider.model.po.SysTenantResource
import io.kudos.ams.sys.provider.dao.SysTenantResourceDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


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

    //endregion your codes 2

}
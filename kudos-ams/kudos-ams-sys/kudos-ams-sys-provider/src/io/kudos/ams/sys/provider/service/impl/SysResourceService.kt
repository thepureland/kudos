package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysResourceService
import io.kudos.ams.sys.provider.model.po.SysResource
import io.kudos.ams.sys.provider.dao.SysResourceDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 资源业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysResourceService : BaseCrudService<String, SysResource, SysResourceDao>(), ISysResourceService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
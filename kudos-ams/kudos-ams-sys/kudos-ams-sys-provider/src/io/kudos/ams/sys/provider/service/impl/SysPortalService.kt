package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysPortalService
import io.kudos.ams.sys.provider.model.po.SysPortal
import io.kudos.ams.sys.provider.dao.SysPortalDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 门户业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysPortalService : BaseCrudService<String, SysPortal, SysPortalDao>(), ISysPortalService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
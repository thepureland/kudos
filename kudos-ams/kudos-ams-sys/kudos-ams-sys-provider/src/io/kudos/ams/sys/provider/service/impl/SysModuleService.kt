package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysModuleService
import io.kudos.ams.sys.provider.model.po.SysModule
import io.kudos.ams.sys.provider.dao.SysModuleDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 模块业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysModuleService : BaseCrudService<String, SysModule, SysModuleDao>(), ISysModuleService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
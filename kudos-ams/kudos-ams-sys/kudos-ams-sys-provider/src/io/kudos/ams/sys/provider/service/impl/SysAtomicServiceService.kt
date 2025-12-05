package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysAtomicServiceService
import io.kudos.ams.sys.provider.model.po.SysAtomicService
import io.kudos.ams.sys.provider.dao.SysAtomicServiceDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 原子服务业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysAtomicServiceService : BaseCrudService<String, SysAtomicService, SysAtomicServiceDao>(), ISysAtomicServiceService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysMicroServiceService
import io.kudos.ams.sys.provider.model.po.SysMicroService
import io.kudos.ams.sys.provider.dao.SysMicroServiceDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 微服务业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysMicroServiceService : BaseCrudService<String, SysMicroService, SysMicroServiceDao>(), ISysMicroServiceService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysSubSystemMicroServiceService
import io.kudos.ams.sys.provider.model.po.SysSubSystemMicroService
import io.kudos.ams.sys.provider.dao.SysSubSystemMicroServiceDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 子系统-微服务关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysSubSystemMicroServiceService : BaseCrudService<String, SysSubSystemMicroService, SysSubSystemMicroServiceDao>(), ISysSubSystemMicroServiceService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
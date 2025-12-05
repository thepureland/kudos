package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysSubSystemService
import io.kudos.ams.sys.provider.model.po.SysSubSystem
import io.kudos.ams.sys.provider.dao.SysSubSystemDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 子系统业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysSubSystemService : BaseCrudService<String, SysSubSystem, SysSubSystemDao>(), ISysSubSystemService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
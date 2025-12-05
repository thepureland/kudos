package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysDomainService
import io.kudos.ams.sys.provider.model.po.SysDomain
import io.kudos.ams.sys.provider.dao.SysDomainDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 域名业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysDomainService : BaseCrudService<String, SysDomain, SysDomainDao>(), ISysDomainService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
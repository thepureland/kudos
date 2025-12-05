package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysAccessRuleService
import io.kudos.ams.sys.provider.model.po.SysAccessRule
import io.kudos.ams.sys.provider.dao.SysAccessRuleDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 访问规则业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysAccessRuleService : BaseCrudService<String, SysAccessRule, SysAccessRuleDao>(), ISysAccessRuleService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysAccessRuleBiz
import io.kudos.ams.sys.service.model.po.SysAccessRule
import io.kudos.ams.sys.service.dao.SysAccessRuleDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 访问规则业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysAccessRuleBiz : BaseCrudBiz<String, SysAccessRule, SysAccessRuleDao>(), ISysAccessRuleBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
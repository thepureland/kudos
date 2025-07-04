package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysAccessRuleIpBiz
import io.kudos.ams.sys.service.model.po.SysAccessRuleIp
import io.kudos.ams.sys.service.dao.SysAccessRuleIpDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * ip访问规则业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysAccessRuleIpBiz : BaseCrudBiz<String, SysAccessRuleIp, SysAccessRuleIpDao>(), ISysAccessRuleIpBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
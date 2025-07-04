package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysTenantLanguageBiz
import io.kudos.ams.sys.service.model.po.SysTenantLanguage
import io.kudos.ams.sys.service.dao.SysTenantLanguageDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 租户-语言关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantLanguageBiz : BaseCrudBiz<String, SysTenantLanguage, SysTenantLanguageDao>(), ISysTenantLanguageBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
package io.kudos.ams.sys.service.dao

import io.kudos.ams.sys.service.model.po.SysTenantLanguage
import io.kudos.ams.sys.service.model.table.SysTenantLanguages
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 租户-语言关系数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysTenantLanguageDao : BaseCrudDao<String, SysTenantLanguage, SysTenantLanguages>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
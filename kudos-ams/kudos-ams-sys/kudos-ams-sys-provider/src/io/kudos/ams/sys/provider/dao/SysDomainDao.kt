package io.kudos.ams.sys.provider.dao

import io.kudos.ams.sys.provider.model.po.SysDomain
import io.kudos.ams.sys.provider.model.table.SysDomains
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 域名数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysDomainDao : BaseCrudDao<String, SysDomain, SysDomains>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
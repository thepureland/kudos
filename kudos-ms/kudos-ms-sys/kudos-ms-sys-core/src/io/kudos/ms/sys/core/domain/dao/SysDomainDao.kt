package io.kudos.ms.sys.core.domain.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.domain.model.po.SysDomain
import io.kudos.ms.sys.core.domain.model.table.SysDomains
import org.springframework.stereotype.Repository


/**
 * 域名数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysDomainDao : BaseCrudDao<String, SysDomain, SysDomains>() {



}
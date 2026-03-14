package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.model.po.SysCache
import io.kudos.ms.sys.core.model.table.SysCaches
import org.springframework.stereotype.Repository


/**
 * 缓存数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysCacheDao : BaseCrudDao<String, SysCache, SysCaches>() {



}
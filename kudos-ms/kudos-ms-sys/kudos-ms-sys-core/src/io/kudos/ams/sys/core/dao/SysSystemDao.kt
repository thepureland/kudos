package io.kudos.ms.sys.core.dao

import io.kudos.ms.sys.core.model.po.SysSystem
import io.kudos.ms.sys.core.model.table.SysSystems
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 系统数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysSystemDao : BaseCrudDao<String, SysSystem, SysSystems>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
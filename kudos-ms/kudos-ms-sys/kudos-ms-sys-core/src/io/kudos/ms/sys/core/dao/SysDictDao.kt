package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.model.po.SysDict
import io.kudos.ms.sys.core.model.table.SysDicts
import org.springframework.stereotype.Repository


/**
 * 字典数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysDictDao : BaseCrudDao<String, SysDict, SysDicts>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
package io.kudos.ams.sys.provider.dao

import io.kudos.ams.sys.provider.model.po.SysModule
import io.kudos.ams.sys.provider.model.table.SysModules
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 模块数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysModuleDao : BaseCrudDao<String, SysModule, SysModules>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
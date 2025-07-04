package io.kudos.ams.sys.service.dao

import io.kudos.ams.sys.service.model.po.SysSubSystem
import io.kudos.ams.sys.service.model.table.SysSubSystems
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 子系统数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysSubSystemDao : BaseCrudDao<String, SysSubSystem, SysSubSystems>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
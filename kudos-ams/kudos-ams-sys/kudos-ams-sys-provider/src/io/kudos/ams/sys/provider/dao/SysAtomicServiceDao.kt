package io.kudos.ams.sys.provider.dao

import io.kudos.ams.sys.provider.model.po.SysAtomicService
import io.kudos.ams.sys.provider.model.table.SysAtomicServices
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 原子服务数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysAtomicServiceDao : BaseCrudDao<String, SysAtomicService, SysAtomicServices>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
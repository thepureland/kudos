package io.kudos.ams.sys.service.dao

import io.kudos.ams.sys.service.model.po.SysDataSource
import io.kudos.ams.sys.service.model.table.SysDataSources
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 数据源数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysDataSourceDao : BaseCrudDao<String, SysDataSource, SysDataSources>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
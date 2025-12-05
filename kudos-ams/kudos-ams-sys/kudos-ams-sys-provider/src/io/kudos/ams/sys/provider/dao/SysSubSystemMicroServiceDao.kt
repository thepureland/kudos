package io.kudos.ams.sys.provider.dao

import io.kudos.ams.sys.provider.model.po.SysSubSystemMicroService
import io.kudos.ams.sys.provider.model.table.SysSubSystemMicroServices
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 子系统-微服务关系数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysSubSystemMicroServiceDao : BaseCrudDao<String, SysSubSystemMicroService, SysSubSystemMicroServices>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
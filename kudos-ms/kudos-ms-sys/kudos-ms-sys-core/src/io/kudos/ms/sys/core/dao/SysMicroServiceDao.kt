package io.kudos.ms.sys.core.dao

import io.kudos.ms.sys.core.model.po.SysMicroService
import io.kudos.ms.sys.core.model.table.SysMicroServices
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 微服务数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysMicroServiceDao : BaseCrudDao<String, SysMicroService, SysMicroServices>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
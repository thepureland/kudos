package io.kudos.ams.sys.provider.dao

import io.kudos.ams.sys.provider.model.po.SysMicroServiceAtomicService
import io.kudos.ams.sys.provider.model.table.SysMicroServiceAtomicServices
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 微服务-原子服务关系数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysMicroServiceAtomicServiceDao : BaseCrudDao<String, SysMicroServiceAtomicService, SysMicroServiceAtomicServices>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
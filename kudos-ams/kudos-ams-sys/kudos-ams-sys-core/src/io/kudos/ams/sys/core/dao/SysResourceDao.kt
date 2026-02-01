package io.kudos.ams.sys.core.dao

import io.kudos.ams.sys.core.model.po.SysResource
import io.kudos.ams.sys.core.model.table.SysResources
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 资源数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysResourceDao : BaseCrudDao<String, SysResource, SysResources>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
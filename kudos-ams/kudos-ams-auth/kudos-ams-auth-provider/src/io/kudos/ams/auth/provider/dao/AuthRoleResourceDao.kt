package io.kudos.ams.auth.provider.dao

import io.kudos.ams.auth.provider.model.po.AuthRoleResource
import io.kudos.ams.auth.provider.model.table.AuthRoleResources
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 角色-资源关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthRoleResourceDao : BaseCrudDao<String, AuthRoleResource, AuthRoleResources>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

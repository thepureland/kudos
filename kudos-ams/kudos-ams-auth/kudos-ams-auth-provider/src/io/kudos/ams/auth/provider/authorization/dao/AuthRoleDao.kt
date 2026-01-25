package io.kudos.ams.auth.provider.authorization.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ams.auth.provider.authorization.model.po.AuthRole
import io.kudos.ams.auth.provider.authorization.model.table.AuthRoles
import org.springframework.stereotype.Repository


/**
 * 角色数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthRoleDao : BaseCrudDao<String, AuthRole, AuthRoles>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

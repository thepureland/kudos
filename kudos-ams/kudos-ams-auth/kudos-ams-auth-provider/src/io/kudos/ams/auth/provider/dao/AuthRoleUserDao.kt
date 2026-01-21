package io.kudos.ams.auth.provider.dao

import io.kudos.ams.auth.provider.model.po.AuthRoleUser
import io.kudos.ams.auth.provider.model.table.AuthRoleUsers
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 角色-用户关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthRoleUserDao : BaseCrudDao<String, AuthRoleUser, AuthRoleUsers>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

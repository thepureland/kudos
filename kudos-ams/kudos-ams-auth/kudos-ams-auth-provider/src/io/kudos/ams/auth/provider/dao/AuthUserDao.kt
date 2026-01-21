package io.kudos.ams.auth.provider.dao

import io.kudos.ams.auth.provider.model.po.AuthUser
import io.kudos.ams.auth.provider.model.table.AuthUsers
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 用户数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthUserDao : BaseCrudDao<String, AuthUser, AuthUsers>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

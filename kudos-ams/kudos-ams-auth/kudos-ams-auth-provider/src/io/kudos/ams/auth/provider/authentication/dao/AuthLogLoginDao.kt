package io.kudos.ams.auth.provider.authentication.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ams.auth.provider.authentication.model.po.AuthLogLogin
import io.kudos.ams.auth.provider.authentication.model.table.AuthLogLogins
import org.springframework.stereotype.Repository

/**
 * 登录日志数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthLogLoginDao : BaseCrudDao<String, AuthLogLogin, AuthLogLogins>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
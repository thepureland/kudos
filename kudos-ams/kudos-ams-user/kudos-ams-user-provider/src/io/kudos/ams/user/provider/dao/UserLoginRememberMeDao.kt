package io.kudos.ams.user.provider.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ams.user.provider.model.po.UserLoginRememberMe
import io.kudos.ams.user.provider.model.table.UserLoginRememberMes
import org.springframework.stereotype.Repository


/**
 * 记住我登录数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class UserLoginRememberMeDao : BaseCrudDao<String, UserLoginRememberMe, UserLoginRememberMes>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

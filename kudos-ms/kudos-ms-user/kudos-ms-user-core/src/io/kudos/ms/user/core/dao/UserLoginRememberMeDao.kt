package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.user.core.model.po.UserLoginRememberMe
import io.kudos.ms.user.core.model.table.UserLoginRememberMes
import org.springframework.stereotype.Repository


/**
 * 记住我登录数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class UserLoginRememberMeDao : BaseCrudDao<String, UserLoginRememberMe, UserLoginRememberMes>() {



}

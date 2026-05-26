package io.kudos.ms.user.core.login.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.user.core.login.model.po.UserLoginRememberMe
import io.kudos.ms.user.core.login.model.table.UserLoginRememberMes
import org.springframework.stereotype.Repository


/**
 * Remember-me login data access object
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class UserLoginRememberMeDao : BaseCrudDao<String, UserLoginRememberMe, UserLoginRememberMes>() {



}

package io.kudos.ms.user.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ms.user.core.dao.UserLoginRememberMeDao
import io.kudos.ms.user.core.model.po.UserLoginRememberMe
import io.kudos.ms.user.core.service.iservice.IUserLoginRememberMeService
import org.springframework.stereotype.Service


/**
 * 记住我登录业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
//region your codes 1
open class UserLoginRememberMeService : BaseCrudService<String, UserLoginRememberMe, UserLoginRememberMeDao>(), IUserLoginRememberMeService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

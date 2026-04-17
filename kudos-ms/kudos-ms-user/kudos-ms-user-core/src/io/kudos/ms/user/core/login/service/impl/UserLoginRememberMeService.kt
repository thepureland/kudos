package io.kudos.ms.user.core.login.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.user.core.login.dao.UserLoginRememberMeDao
import io.kudos.ms.user.core.login.model.po.UserLoginRememberMe
import io.kudos.ms.user.core.login.service.iservice.IUserLoginRememberMeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 记住我登录业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class UserLoginRememberMeService(
    dao: UserLoginRememberMeDao
) : BaseCrudService<String, UserLoginRememberMe, UserLoginRememberMeDao>(dao), IUserLoginRememberMeService {



}

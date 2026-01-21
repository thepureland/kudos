package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.provider.service.iservice.IAuthUserService
import io.kudos.ams.auth.provider.model.po.AuthUser
import io.kudos.ams.auth.provider.dao.AuthUserDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 用户业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthUserService : BaseCrudService<String, AuthUser, AuthUserDao>(), IAuthUserService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

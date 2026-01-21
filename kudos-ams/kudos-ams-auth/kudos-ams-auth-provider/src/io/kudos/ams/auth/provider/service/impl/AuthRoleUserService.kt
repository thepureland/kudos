package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.provider.service.iservice.IAuthRoleUserService
import io.kudos.ams.auth.provider.model.po.AuthRoleUser
import io.kudos.ams.auth.provider.dao.AuthRoleUserDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 角色-用户关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthRoleUserService : BaseCrudService<String, AuthRoleUser, AuthRoleUserDao>(), IAuthRoleUserService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.provider.service.iservice.IAuthRoleService
import io.kudos.ams.auth.provider.model.po.AuthRole
import io.kudos.ams.auth.provider.dao.AuthRoleDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 角色业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthRoleService : BaseCrudService<String, AuthRole, AuthRoleDao>(), IAuthRoleService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

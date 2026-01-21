package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.provider.service.iservice.IAuthDeptUserService
import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.ams.auth.provider.dao.AuthDeptUserDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 部门-用户关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthDeptUserService : BaseCrudService<String, AuthDeptUser, AuthDeptUserDao>(), IAuthDeptUserService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

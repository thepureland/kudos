package io.kudos.ams.auth.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ams.auth.core.dao.AuthGroupDao
import io.kudos.ams.auth.core.model.po.AuthGroup
import io.kudos.ams.auth.core.service.iservice.IAuthGroupService
import org.springframework.stereotype.Service


/**
 * 用户组业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthGroupService : BaseCrudService<String, AuthGroup, AuthGroupDao>(), IAuthGroupService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

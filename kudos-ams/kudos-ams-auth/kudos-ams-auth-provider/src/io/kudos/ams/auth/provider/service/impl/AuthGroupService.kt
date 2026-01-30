package io.kudos.ams.auth.provider.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.ams.auth.provider.dao.AuthGroupDao
import io.kudos.ams.auth.provider.model.po.AuthGroup
import io.kudos.ams.auth.provider.service.iservice.IAuthGroupService
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

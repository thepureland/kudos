package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.provider.service.iservice.IAuthRoleResourceService
import io.kudos.ams.auth.provider.model.po.AuthRoleResource
import io.kudos.ams.auth.provider.dao.AuthRoleResourceDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 角色-资源关系业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthRoleResourceService : BaseCrudService<String, AuthRoleResource, AuthRoleResourceDao>(), IAuthRoleResourceService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

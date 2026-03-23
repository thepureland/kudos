package io.kudos.ms.auth.core.service.impl

import io.kudos.base.support.service.BaseCrudService
import io.kudos.ms.auth.core.dao.AuthGroupDao
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.ms.auth.core.service.iservice.IAuthGroupService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 用户组业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthGroupService(
    dao: AuthGroupDao
) : BaseCrudService<String, AuthGroup, AuthGroupDao>(dao), IAuthGroupService {



}

package io.kudos.ms.user.core.account.service.impl

import io.kudos.ms.user.core.security.UserOwnedCrudService
import io.kudos.ms.user.core.account.dao.UserAccountProtectionDao
import io.kudos.ms.user.core.account.model.po.UserAccountProtection
import io.kudos.ms.user.core.account.service.iservice.IUserAccountProtectionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * User account protection service implementation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class UserAccountProtectionService(
    dao: UserAccountProtectionDao
) : UserOwnedCrudService<String, UserAccountProtection, UserAccountProtectionDao>(dao), IUserAccountProtectionService {



}

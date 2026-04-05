package io.kudos.ms.user.core.protection.service.impl
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.user.core.protection.dao.UserAccountProtectionDao
import io.kudos.ms.user.core.protection.model.po.UserAccountProtection
import io.kudos.ms.user.core.protection.service.iservice.IUserAccountProtectionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 用户账号保护业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class UserAccountProtectionService(
    dao: UserAccountProtectionDao
) : BaseCrudService<String, UserAccountProtection, UserAccountProtectionDao>(dao), IUserAccountProtectionService {



}

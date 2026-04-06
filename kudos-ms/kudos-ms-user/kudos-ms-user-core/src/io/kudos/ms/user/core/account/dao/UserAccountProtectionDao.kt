package io.kudos.ms.user.core.account.dao
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.user.core.account.model.po.UserAccountProtection
import io.kudos.ms.user.core.account.model.table.UserAccountProtections
import org.springframework.stereotype.Repository


/**
 * 用户账号保护数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class UserAccountProtectionDao : BaseCrudDao<String, UserAccountProtection, UserAccountProtections>() {



}

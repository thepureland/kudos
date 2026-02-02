package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.user.core.model.po.UserAccount
import io.kudos.ms.user.core.model.table.UserAccounts
import org.springframework.stereotype.Repository


/**
 * 用户数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class UserAccountDao : BaseCrudDao<String, UserAccount, UserAccounts>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

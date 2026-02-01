package io.kudos.ams.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ams.auth.core.model.po.AuthGroup
import io.kudos.ams.auth.core.model.table.AuthGroups
import org.springframework.stereotype.Repository


/**
 * 用户组数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthGroupDao : BaseCrudDao<String, AuthGroup, AuthGroups>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}

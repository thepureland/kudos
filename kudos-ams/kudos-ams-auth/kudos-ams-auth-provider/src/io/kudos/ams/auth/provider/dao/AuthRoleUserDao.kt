package io.kudos.ams.auth.provider.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ams.auth.provider.model.po.AuthRoleUser
import io.kudos.ams.auth.provider.model.table.AuthRoleUsers
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.stereotype.Repository


/**
 * 角色-用户关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthRoleUserDao : BaseCrudDao<String, AuthRoleUser, AuthRoleUsers>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 检查关系是否存在
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, userId: String): Boolean {
        val criteria = Criteria.of(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
            .addAnd(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        return count(criteria) > 0
    }

    //endregion your codes 2

}

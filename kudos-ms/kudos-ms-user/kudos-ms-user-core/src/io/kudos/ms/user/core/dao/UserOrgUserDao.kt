package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.user.core.model.po.UserOrgUser
import io.kudos.ms.user.core.model.table.UserOrgUsers
import org.springframework.stereotype.Repository


/**
 * 机构-用户关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class UserOrgUserDao : BaseCrudDao<String, UserOrgUser, UserOrgUsers>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 检查关系是否存在
     *
     * @param orgId 机构ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(orgId: String, userId: String): Boolean {
        val criteria = Criteria.of(UserOrgUser::orgId.name, OperatorEnum.EQ, orgId)
            .addAnd(UserOrgUser::userId.name, OperatorEnum.EQ, userId)
        return count(criteria) > 0
    }

    //endregion your codes 2

}

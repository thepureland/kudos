package io.kudos.ams.auth.provider.dao

import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.ams.auth.provider.model.table.AuthDeptUsers
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 部门-用户关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthDeptUserDao : BaseCrudDao<String, AuthDeptUser, AuthDeptUsers>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 检查关系是否存在
     *
     * @param deptId 部门ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(deptId: String, userId: String): Boolean {
        val criteria = Criteria.of(AuthDeptUser::deptId.name, OperatorEnum.EQ, deptId)
            .addAnd(AuthDeptUser::userId.name, OperatorEnum.EQ, userId)
        return count(criteria) > 0
    }

    //endregion your codes 2

}

package io.kudos.ams.auth.provider.service.impl

import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.cache.UserByIdCacheHandler
import io.kudos.ams.auth.provider.dao.AuthDeptDao
import io.kudos.ams.auth.provider.dao.AuthDeptUserDao
import io.kudos.ams.auth.provider.model.po.AuthDept
import io.kudos.ams.auth.provider.model.po.AuthDeptUser
import io.kudos.ams.auth.provider.service.iservice.IAuthDeptService
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


/**
 * 部门业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class AuthDeptService : BaseCrudService<String, AuthDept, AuthDeptDao>(), IAuthDeptService {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var authDeptUserDao: AuthDeptUserDao

    @Autowired
    private lateinit var userByIdCacheHandler: UserByIdCacheHandler

    override fun getDeptAdmins(deptId: String): List<AuthUserCacheItem> {
        // 查询部门管理员用户ID列表
        val criteria = Criteria(AuthDeptUser::deptId.name, OperatorEnum.EQ, deptId)
            .addAnd(AuthDeptUser::deptAdmin.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val adminUserIds = authDeptUserDao.searchProperty(criteria, AuthDeptUser::userId.name) as List<String>
        
        // 如果没有管理员，直接返回空列表
        if (adminUserIds.isEmpty()) {
            return emptyList()
        }
        
        // 批量获取用户信息
        val usersMap = userByIdCacheHandler.getUsersByIds(adminUserIds)
        
        // 返回用户列表（按原始ID顺序）
        return adminUserIds.mapNotNull { usersMap[it] }
    }

    //endregion your codes 2

}

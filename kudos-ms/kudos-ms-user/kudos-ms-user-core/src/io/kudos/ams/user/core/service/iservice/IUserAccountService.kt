package io.kudos.ms.user.core.service.iservice

import io.kudos.ms.sys.common.vo.resource.SysResourceCacheItem
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.common.vo.user.UserAccountRecord
import io.kudos.ms.user.core.model.po.UserAccount
import io.kudos.base.support.iservice.IBaseCrudService
import java.time.LocalDateTime


/**
 * 用户业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IUserAccountService : IBaseCrudService<String, UserAccount> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据用户ID获取该用户所属的所有机构ID列表
     *
     * @param userId 用户ID
     * @return List<String> 机构ID列表，如果用户不存在或没有机构则返回空列表
     */
    fun getUserOrgIds(userId: String): List<String>

    /**
     * 根据租户ID获取该租户下所有激活用户的ID列表
     * 只返回active=true的用户ID
     *
     * @param tenantId 租户ID
     * @return List<String> 用户ID列表
     */
    fun getUserIds(tenantId: String): List<String>



    /**
     * 根据用户ID获取该用户所属的所有机构列表
     *
     * @param userId 用户ID
     * @return List<UserOrgCacheItem> 机构列表，如果用户不存在或没有机构则返回空列表
     */
    fun getUserOrgs(userId: String): List<UserOrgCacheItem>



    /**
     * 检查用户是否属于指定机构
     *
     * @param userId 用户ID
     * @param orgId 机构ID
     * @return true表示用户属于该机构，false表示不属于
     */
    fun isUserInOrg(userId: String, orgId: String): Boolean



    /**
     * 根据租户ID和用户名获取用户信息
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户缓存项，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheItem?

    /**
     * 根据ID获取用户记录（非缓存）
     *
     * @param id 用户ID
     * @return 用户记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserRecord(id: String): UserAccountRecord?

    /**
     * 根据租户ID获取用户列表
     *
     * @param tenantId 租户ID
     * @return 用户记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUsersByTenantId(tenantId: String): List<UserAccountRecord>

    /**
     * 根据机构ID获取用户列表
     *
     * @param orgId 机构ID
     * @return 用户记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUsersByOrgId(orgId: String): List<UserAccountRecord>



    /**
     * 更新用户启用状态
     *
     * @param id 用户ID
     * @param active 是否启用
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 重置登录密码
     *
     * @param id 用户ID
     * @param newPassword 新密码（明文）
     * @return 是否重置成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun resetPassword(id: String, newPassword: String): Boolean

    /**
     * 重置安全密码
     *
     * @param id 用户ID
     * @param newPassword 新密码（明文）
     * @return 是否重置成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun resetSecurityPassword(id: String, newPassword: String): Boolean

    /**
     * 更新最后登录信息
     *
     * @param id 用户ID
     * @param loginIp 登录IP
     * @param loginTime 登录时间
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateLastLoginInfo(id: String, loginIp: Long, loginTime: LocalDateTime): Boolean

    /**
     * 更新最后登出信息
     *
     * @param id 用户ID
     * @param logoutTime 登出时间
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateLastLogoutInfo(id: String, logoutTime: LocalDateTime): Boolean

    /**
     * 增加登录错误次数
     *
     * @param id 用户ID
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun incrementLoginErrorTimes(id: String): Boolean

    /**
     * 重置登录错误次数
     *
     * @param id 用户ID
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun resetLoginErrorTimes(id: String): Boolean

    /**
     * 增加安全密码错误次数
     *
     * @param id 用户ID
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun incrementSecurityPasswordErrorTimes(id: String): Boolean

    /**
     * 重置安全密码错误次数
     *
     * @param id 用户ID
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun resetSecurityPasswordErrorTimes(id: String): Boolean

    //endregion your codes 2

}

package io.kudos.ms.user.core.account.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.account.vo.response.AuthKeySetup
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.core.account.model.po.UserAccount
import java.time.LocalDateTime


/**
 * 用户业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserAccountService : IBaseCrudService<String, UserAccount> {


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
     * @return List<UserOrgCacheEntry> 机构列表，如果用户不存在或没有机构则返回空列表
     */
    fun getUserOrgs(userId: String): List<UserOrgCacheEntry>



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
    fun getUserByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheEntry?

    /**
     * 根据ID获取用户记录（非缓存）
     *
     * @param id 用户ID
     * @return 用户记录，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserRecord(id: String): UserAccountRow?

    /**
     * 根据租户ID获取用户列表
     *
     * @param tenantId 租户ID
     * @return 用户记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUsersByTenantId(tenantId: String): List<UserAccountRow>

    /**
     * 根据机构ID获取用户列表
     *
     * @param orgId 机构ID
     * @return 用户记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUsersByOrgId(orgId: String): List<UserAccountRow>



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

    /**
     * 为指定用户生成新的 TOTP secret 并落库（覆盖旧值），同时返回 secret + otpauth URL。
     *
     * 用户用 Google Authenticator 等扫码后即可启用二次验证。
     *
     * @param id 用户主键
     * @param accountName 出现在 OTP App 里的账号显示名（一般用 username）
     * @param issuer 出现在 OTP App 里的发行方显示名（一般用应用名，如 "kudos"）
     * @return [AuthKeySetup] 含 secret + otpauthUrl；用户不存在或写库失败返回 null
     */
    fun resetAuthKey(id: String, accountName: String, issuer: String): AuthKeySetup?

    /**
     * 清除用户的 TOTP secret（关闭二次验证）。
     *
     * @param id 用户主键
     * @return 是否更新成功
     */
    fun cleanAuthKey(id: String): Boolean

    /**
     * 校验用户提供的 6 位 TOTP 验证码。
     *
     * @param id 用户主键
     * @param code 用户当前 OTP App 显示的 6 位数字（前导零可丢失，Long 表示）
     * @return true 匹配；false 不匹配或用户未启用 OTP
     */
    fun verifyAuthCode(id: String, code: Long): Boolean

    /**
     * 冻结账号：写入 freeze_* 6 列。`freeze_time` 由实现取 [LocalDateTime.now]。
     *
     * 登录判定逻辑：当 freeze_type IS NOT NULL 且
     * (freeze_start_time IS NULL 或 now >= freeze_start_time) 且
     * (freeze_end_time IS NULL 或 now < freeze_end_time) 时视为"当前冻结"，登录被拒。
     *
     * @param id 用户主键
     * @param freezeType 冻结类型字典码（manual / auto / admin / scheduled 等）
     * @param freezeTitle 简短标题；可空
     * @param freezeContent 详细说明；可空
     * @param freezeStartTime 生效起点；null = 立即生效
     * @param freezeEndTime 失效时刻；null = 永久冻结
     * @return 是否更新成功
     */
    fun freezeAccount(
        id: String,
        freezeType: String,
        freezeTitle: String?,
        freezeContent: String?,
        freezeStartTime: LocalDateTime?,
        freezeEndTime: LocalDateTime?,
    ): Boolean

    /**
     * 解除冻结：清空全部 freeze_* 6 列。
     *
     * @param id 用户主键
     * @return 是否更新成功
     */
    fun unfreezeAccount(id: String): Boolean

    /**
     * 扫描并清理已过期的冻结记录：`freeze_end_time IS NOT NULL AND freeze_end_time < now()`。
     *
     * 注意：登录判定本身已经能识别"过期窗口外的冻结"放行（参见 `PassportService.isCurrentlyFrozen`），
     * 所以此方法**仅作清理**，不影响功能正确性。意义在于：
     *   - 让 admin 列表展示干净（不再看到"已过期但仍标记冻结"的脏数据）
     *   - 避免缓存里的 freeze_* 字段长期持有无意义数据
     *
     * 调度策略由调用方决定——可由 `AutoUnfreezeScheduler` 自动跑（前提：消费方启用
     * `@EnableScheduling`），也可由管理端手动触发。
     *
     * @return 清理掉的账号数量
     */
    fun cleanExpiredFreezes(): Int


}

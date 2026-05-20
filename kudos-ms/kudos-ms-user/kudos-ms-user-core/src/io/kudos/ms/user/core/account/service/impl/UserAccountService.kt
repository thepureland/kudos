package io.kudos.ms.user.core.account.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.lt
import io.kudos.base.security.GoogleAuthenticator
import io.kudos.base.security.PasswordKit
import io.kudos.ms.user.common.account.vo.response.AuthKeySetup
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.account.vo.request.UserAccountQuery
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAccountInserted
import io.kudos.ms.user.core.account.event.UserAccountUpdated
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import jakarta.annotation.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * 用户账号业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class UserAccountService(
    dao: UserAccountDao,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, UserAccount, UserAccountDao>(dao), IUserAccountService {




    @Resource
    private lateinit var userOrgHashCache: UserOrgHashCache

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var orgIdsByUserIdCache: OrgIdsByUserIdCache

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getUserOrgIds(userId: String): List<String> = orgIdsByUserIdCache.getOrgIds(userId)


    @Transactional(readOnly = true)
    override fun getUserIds(tenantId: String): List<String> = dao.searchActiveUserIdsByTenantId(tenantId)


    @Transactional(readOnly = true)
    override fun getUserOrgs(userId: String): List<UserOrgCacheEntry> {
        val orgIds = getUserOrgIds(userId)
        if (orgIds.isEmpty()) return emptyList()
        val orgsMap = userOrgHashCache.getOrgsByIds(orgIds)
        return orgIds.mapNotNull { orgsMap[it] }
    }

    @Transactional(readOnly = true)
    override fun isUserInOrg(userId: String, orgId: String): Boolean = orgId in getUserOrgIds(userId)

    @Transactional(readOnly = true)
    override fun getUserByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheEntry? =
        userAccountHashCache.getUsersByTenantIdAndUsername(tenantId, username)?.id
            ?.let { userAccountHashCache.getUserById(it) }

    @Transactional(readOnly = true)
    override fun getUserRecord(id: String): UserAccountRow? = dao.getAs<UserAccountRow>(id)

    @Transactional(readOnly = true)
    override fun getUsersByTenantId(tenantId: String): List<UserAccountRow> =
        @Suppress("UNCHECKED_CAST")
        dao.search(UserAccountQuery(tenantId = tenantId), UserAccountRow::class)

    @Transactional(readOnly = true)
    override fun getUsersByOrgId(orgId: String): List<UserAccountRow> =
        @Suppress("UNCHECKED_CAST")
        dao.search(UserAccountQuery(orgId = orgId), UserAccountRow::class)

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean =
        updateAndPublish(id, "更新id为${id}的用户的启用状态为${active}") {
            this.active = active
        }

    @Transactional
    override fun resetPassword(id: String, newPassword: String): Boolean {
        val encryptedPassword = PasswordKit.hash(newPassword)
        return updateAndPublish(id, "重置id为${id}的用户的登录密码") {
            this.loginPassword = encryptedPassword
            this.loginErrorTimes = 0
        }
    }

    @Transactional
    override fun resetSecurityPassword(id: String, newPassword: String): Boolean {
        val encryptedPassword = PasswordKit.hash(newPassword)
        return updateAndPublish(id, "重置id为${id}的用户的安全密码") {
            this.securityPassword = encryptedPassword
            this.securityPasswordErrorTimes = 0
        }
    }

    @Transactional
    override fun updateLastLoginInfo(id: String, loginIp: Long, loginTime: LocalDateTime): Boolean =
        updateAndPublish(id, "更新id为${id}的用户的最后登录信息") {
            this.lastLoginIp = loginIp
            this.lastLoginTime = loginTime
            this.loginErrorTimes = 0
        }

    @Transactional
    override fun updateLastLogoutInfo(id: String, logoutTime: LocalDateTime): Boolean =
        updateAndPublish(id, "更新id为${id}的用户的最后登出信息") {
            this.lastLogoutTime = logoutTime
        }

    @Transactional
    override fun incrementLoginErrorTimes(id: String): Boolean {
        val existing = dao.get(id) ?: return false
        val current = existing.loginErrorTimes ?: 0
        return updateAndPublish(id, "增加id为${id}的用户的登录错误次数") {
            this.loginErrorTimes = current + 1
        }
    }

    @Transactional
    override fun resetLoginErrorTimes(id: String): Boolean =
        updateAndPublish(id, "重置id为${id}的用户的登录错误次数") {
            this.loginErrorTimes = 0
        }

    @Transactional
    override fun incrementSecurityPasswordErrorTimes(id: String): Boolean {
        val existing = dao.get(id) ?: return false
        val current = existing.securityPasswordErrorTimes ?: 0
        return updateAndPublish(id, "增加id为${id}的用户的安全密码错误次数") {
            this.securityPasswordErrorTimes = current + 1
        }
    }

    @Transactional
    override fun resetSecurityPasswordErrorTimes(id: String): Boolean =
        updateAndPublish(id, "重置id为${id}的用户的安全密码错误次数") {
            this.securityPasswordErrorTimes = 0
        }

    /**
     * 共用模板：构造只含 id + 修改字段的 [UserAccount]，调用 [UserAccountDao.update]，
     * 成功记录 debug + 发布 [UserAccountUpdated]，失败记录 error。
     *
     * 抽出来收口原本散布在 9 个 update 方法里的「build → update → log + event」三段式样板，
     * 避免新增字段时漏发事件或日志措辞漂移。
     */
    private inline fun updateAndPublish(id: String, actionDesc: String, build: UserAccount.() -> Unit): Boolean {
        val user = UserAccount { this.id = id }.apply(build)
        val success = dao.update(user)
        if (success) {
            log.debug("$actionDesc。")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.error("${actionDesc}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的用户。")
        eventPublisher.publishEvent(UserAccountInserted(id = id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, UserAccount::id.name) as String
        if (success) {
            log.debug("更新id为${id}的用户。")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.error("更新id为${id}的用户失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val user = dao.get(id) ?: run {
            log.warn("删除id为${id}的用户时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的用户。")
            eventPublisher.publishEvent(UserAccountDeleted(id, user.tenantId, user.username))
        } else {
            log.error("删除id为${id}的用户失败！")
        }
        return success
    }

    @Transactional
    override fun resetAuthKey(id: String, accountName: String, issuer: String): AuthKeySetup? {
        val secret = GoogleAuthenticator.generateSecretKey()
            ?: run {
                log.error("生成 TOTP secret 失败: userId=${id}")
                return null
            }
        val user = UserAccount {
            this.id = id
            this.authenticationKey = secret
        }
        if (!dao.update(user)) {
            log.error("重置 TOTP secret 失败（用户不存在？）: userId=${id}")
            return null
        }
        // 标准 otpauth URL，前端可直接渲染为二维码（zxing/qrcode.js 等）
        val otpauthUrl = "otpauth://totp/${encodeOtpAuthLabel(issuer, accountName)}" +
            "?secret=${secret}&issuer=${java.net.URLEncoder.encode(issuer, Charsets.UTF_8)}"
        log.debug("重置 id 为 ${id} 的用户的 TOTP secret。")
        eventPublisher.publishEvent(UserAccountUpdated(id = id))
        return AuthKeySetup(secret = secret, otpauthUrl = otpauthUrl)
    }

    @Transactional
    override fun cleanAuthKey(id: String): Boolean {
        // ktorm 的 update 对于 null 字段：直接 set null 需要用 dao.updateProperties
        val success = dao.updateProperties(id, mapOf(UserAccount::authenticationKey.name to null))
        if (success) {
            log.debug("清除 id 为 ${id} 的用户的 TOTP secret。")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.warn("清除 TOTP secret 失败（用户不存在？）: userId=$id")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun verifyAuthCode(id: String, code: Long): Boolean {
        val key = dao.get(id)?.authenticationKey ?: return false
        return GoogleAuthenticator().checkCode(key, code, System.currentTimeMillis())
    }

    /**
     * 按 RFC 6238 / Google Authenticator 规范拼出 `otpauth://` URI 的 label 段。
     *
     * 格式约定：`issuer:accountName` 整体做 URL 编码——冒号在编码后会变成 `%3A`，正符合 GA App 解析期望。
     * 如果 issuer 或 accountName 含空格 / 特殊字符（中文用户名常见），不编码会破坏整个 URI。
     *
     * @param issuer 应用方标识（通常是产品名）
     * @param accountName 账号名（用户登录名/邮箱）
     * @return 编码后的 label 段
     * @author K
     * @since 1.0.0
     */
    private fun encodeOtpAuthLabel(issuer: String, accountName: String): String =
        java.net.URLEncoder.encode("$issuer:$accountName", Charsets.UTF_8)

    @Transactional
    override fun freezeAccount(
        id: String,
        freezeType: String,
        freezeTitle: String?,
        freezeContent: String?,
        freezeStartTime: LocalDateTime?,
        freezeEndTime: LocalDateTime?,
    ): Boolean {
        require(freezeType.isNotBlank()) { "freezeType 不能为空" }
        // 用 updateProperties 显式更新（包括 null）。ktorm 普通 update 对 null 字段不生效，
        // 这里需要明确把 start/end 在 caller 没传时清零。
        val success = dao.updateProperties(
            id, mapOf(
                UserAccount::freezeType.name to freezeType,
                UserAccount::freezeTime.name to LocalDateTime.now(),
                UserAccount::freezeStartTime.name to freezeStartTime,
                UserAccount::freezeEndTime.name to freezeEndTime,
                UserAccount::freezeTitle.name to freezeTitle,
                UserAccount::freezeContent.name to freezeContent,
            )
        )
        if (success) {
            log.debug("冻结 id 为 ${id} 的账号，type=${freezeType}")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.warn("冻结账号失败（用户不存在？）: userId=${id}")
        }
        return success
    }

    @Transactional
    override fun unfreezeAccount(id: String): Boolean {
        // 清掉 6 列。freezeTime 也清掉，避免"曾被冻结过"残留误导。
        val success = dao.updateProperties(
            id, mapOf(
                UserAccount::freezeType.name to null,
                UserAccount::freezeTime.name to null,
                UserAccount::freezeStartTime.name to null,
                UserAccount::freezeEndTime.name to null,
                UserAccount::freezeTitle.name to null,
                UserAccount::freezeContent.name to null,
            )
        )
        if (success) {
            log.debug("解除 id 为 ${id} 的账号冻结。")
            eventPublisher.publishEvent(UserAccountUpdated(id = id))
        } else {
            log.warn("解除冻结失败（用户不存在？）: userId=${id}")
        }
        return success
    }

    @Transactional
    override fun cleanExpiredFreezes(): Int {
        // freeze_end_time IS NOT NULL AND freeze_end_time < now()
        // lt 操作符在底层是 SQL `<`，对 NULL 自然不匹配——永久冻结(freeze_end_time=null)不会被清。
        val expired = dao.searchAs<UserAccount>(Criteria(UserAccount::freezeEndTime lt LocalDateTime.now()))
        val cleared = expired.count { unfreezeAccount(it.id) }
        if (cleared > 0) log.info("auto-unfreeze: 共清理 $cleared 条已过期的冻结记录")
        return cleared
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // 先 snapshot tenantId/username，AFTER_COMMIT 后下游 (tenantId, username) 缓存无法回查
        val snapshots = if (ids.isEmpty()) emptyList()
            else dao.getByIds(ids).map { UserAccountBatchDeleted.Item(it.id, it.tenantId, it.username) }
        val count = super.batchDelete(ids)
        log.debug("批量删除用户，期望删除${ids.size}条，实际删除${count}条。")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(UserAccountBatchDeleted(snapshots))
        }
        return count
    }


}

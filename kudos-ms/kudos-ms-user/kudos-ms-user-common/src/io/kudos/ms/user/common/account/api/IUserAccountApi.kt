package io.kudos.ms.user.common.account.api

import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * 用户 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserAccountApi {


    /**
     * 根据id从缓存中获取用户信息，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param id 用户id
     * @return UserAccountCacheEntry, 找不到返回null
     */
    @GetMapping("/api/internal/user/account/getUserById")
    fun getUserById(@RequestParam id: String): UserAccountCacheEntry?

    /**
     * 根据多个id从缓存中批量获取用户信息，缓存中不存在的，从数据库中加载，并回写缓存
     *
     * @param ids 用户id集合
     * @return Map<用户id，UserAccountCacheEntry>
     */
    @PostMapping("/api/internal/user/account/getUsersByIds")
    fun getUsersByIds(@RequestBody ids: Collection<String>): Map<String, UserAccountCacheEntry>

    /**
     * 根据租户ID和用户名从缓存获取对应的用户ID（仅 active=true）
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 用户ID，找不到返回null
     */
    @GetMapping("/api/internal/user/account/getUserId")
    fun getUserId(@RequestParam tenantId: String, @RequestParam username: String): String?

    /**
     * 根据用户ID获取该用户所属的所有机构列表
     *
     * @param userId 用户ID
     * @return 机构列表；用户不存在或没有机构则返回空列表
     */
    @GetMapping("/api/internal/user/account/getUserOrgs")
    fun getUserOrgs(@RequestParam userId: String): List<UserOrgCacheEntry>

    /**
     * 检查用户是否属于指定机构
     *
     * @param userId 用户ID
     * @param orgId 机构ID
     * @return true 表示用户属于该机构
     */
    @GetMapping("/api/internal/user/account/isUserInOrg")
    fun isUserInOrg(@RequestParam userId: String, @RequestParam orgId: String): Boolean

    /**
     * 根据租户ID获取该租户下所有激活用户的ID列表
     *
     * @param tenantId 租户ID
     * @return 用户ID列表
     */
    @GetMapping("/api/internal/user/account/getUserIds")
    fun getUserIds(@RequestParam tenantId: String): List<String>


}

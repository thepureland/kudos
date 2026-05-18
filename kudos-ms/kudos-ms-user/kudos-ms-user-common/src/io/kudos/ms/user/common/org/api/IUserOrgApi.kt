package io.kudos.ms.user.common.org.api

import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * 机构 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserOrgApi {


    /** 按 id 取机构缓存项 */
    @GetMapping("/api/internal/user/org/getOrgById")
    fun getOrgById(@RequestParam id: String): UserOrgCacheEntry?

    /** 批量取机构缓存项 */
    @PostMapping("/api/internal/user/org/getOrgsByIds")
    fun getOrgsByIds(@RequestBody ids: Collection<String>): Map<String, UserOrgCacheEntry>

    /** 按租户取所有 active=true 的机构 ID 列表 */
    @GetMapping("/api/internal/user/org/getOrgIds")
    fun getOrgIds(@RequestParam tenantId: String): List<String>

    /** 按机构 ID 取管理员用户列表 */
    @GetMapping("/api/internal/user/org/getOrgAdmins")
    fun getOrgAdmins(@RequestParam orgId: String): List<UserAccountCacheEntry>

    /** 按机构 ID 取所有用户列表（含管理员） */
    @GetMapping("/api/internal/user/org/getOrgUsers")
    fun getOrgUsers(@RequestParam orgId: String): List<UserAccountCacheEntry>

    /** 检查用户是否属于指定机构 */
    @GetMapping("/api/internal/user/org/isUserInOrg")
    fun isUserInOrg(@RequestParam userId: String, @RequestParam orgId: String): Boolean

    /** 取指定机构的直接子机构列表 */
    @GetMapping("/api/internal/user/org/getChildOrgs")
    fun getChildOrgs(@RequestParam orgId: String): List<UserOrgCacheEntry>

    /** 取指定机构的父机构；无父返回 null */
    @GetMapping("/api/internal/user/org/getParentOrg")
    fun getParentOrg(@RequestParam orgId: String): UserOrgCacheEntry?


}

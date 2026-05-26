package io.kudos.ms.user.common.org.api

import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * Org public API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserOrgApi {


    /** Fetch org cache entry by id */
    @GetMapping("/api/internal/user/org/getOrgById")
    fun getOrgById(@RequestParam id: String): UserOrgCacheEntry?

    /** Batch fetch org cache entries */
    @PostMapping("/api/internal/user/org/getOrgsByIds")
    fun getOrgsByIds(@RequestBody ids: Collection<String>): Map<String, UserOrgCacheEntry>

    /** List all active=true org IDs for the given tenant */
    @GetMapping("/api/internal/user/org/getOrgIds")
    fun getOrgIds(@RequestParam tenantId: String): List<String>

    /** List admin users of the given org */
    @GetMapping("/api/internal/user/org/getOrgAdmins")
    fun getOrgAdmins(@RequestParam orgId: String): List<UserAccountCacheEntry>

    /** List all users in the given org (including admins) */
    @GetMapping("/api/internal/user/org/getOrgUsers")
    fun getOrgUsers(@RequestParam orgId: String): List<UserAccountCacheEntry>

    /** Check whether the user belongs to the given org */
    @GetMapping("/api/internal/user/org/isUserInOrg")
    fun isUserInOrg(@RequestParam userId: String, @RequestParam orgId: String): Boolean

    /** List the direct child orgs of the given org */
    @GetMapping("/api/internal/user/org/getChildOrgs")
    fun getChildOrgs(@RequestParam orgId: String): List<UserOrgCacheEntry>

    /** Fetch the parent org of the given org; returns null if none */
    @GetMapping("/api/internal/user/org/getParentOrg")
    fun getParentOrg(@RequestParam orgId: String): UserOrgCacheEntry?


}

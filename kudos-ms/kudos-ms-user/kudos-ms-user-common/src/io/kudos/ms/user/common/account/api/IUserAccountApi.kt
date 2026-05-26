package io.kudos.ms.user.common.account.api

import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * User external API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserAccountApi {


    /**
     * Get user info from cache by id; if not in cache, load from database and write back to cache.
     *
     * @param id user id
     * @return UserAccountCacheEntry, null if not found
     */
    @GetMapping("/api/internal/user/account/getUserById")
    fun getUserById(@RequestParam id: String): UserAccountCacheEntry?

    /**
     * Batch-get user info from cache by multiple ids; entries missing from cache are loaded from database and written back.
     *
     * @param ids user id collection
     * @return Map<user id, UserAccountCacheEntry>
     */
    @PostMapping("/api/internal/user/account/getUsersByIds")
    fun getUsersByIds(@RequestBody ids: Collection<String>): Map<String, UserAccountCacheEntry>

    /**
     * Get the user ID from cache by tenant ID and username (active=true only).
     *
     * @param tenantId tenant ID
     * @param username username
     * @return user ID, null if not found
     */
    @GetMapping("/api/internal/user/account/getUserId")
    fun getUserId(@RequestParam tenantId: String, @RequestParam username: String): String?

    /**
     * Get all organizations the user belongs to by user ID.
     *
     * @param userId user ID
     * @return organization list; empty list if the user does not exist or has no organizations
     */
    @GetMapping("/api/internal/user/account/getUserOrgs")
    fun getUserOrgs(@RequestParam userId: String): List<UserOrgCacheEntry>

    /**
     * Check whether the user belongs to the specified organization.
     *
     * @param userId user ID
     * @param orgId organization ID
     * @return true if the user belongs to the organization
     */
    @GetMapping("/api/internal/user/account/isUserInOrg")
    fun isUserInOrg(@RequestParam userId: String, @RequestParam orgId: String): Boolean

    /**
     * Get the ID list of all active users under the given tenant.
     *
     * @param tenantId tenant ID
     * @return user ID list
     */
    @GetMapping("/api/internal/user/account/getUserIds")
    fun getUserIds(@RequestParam tenantId: String): List<String>


}

package io.kudos.ms.user.core.org.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.org.vo.response.UserOrgTreeRow
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.org.model.po.UserOrg


/**
 * Organization business interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserOrgService : IBaseCrudService<String, UserOrg> {


    /**
     * Gets all administrator user info of the organization by organization ID.
     *
     * @param orgId organization ID
     * @return List<UserAccountCacheEntry> list of organization administrator users; returns an empty list if there are no administrators
     */
    fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry>

    /**
     * Gets the list of user IDs (including administrators and regular users, deduplicated) under the organization
     * and all its enabled descendant organizations by organization ID.
     *
     * Business scenario: "Sales Director" queries "Sales Department" -> should include members of sub-organizations
     * such as "Sales Department/East China".
     * If you only need members directly attached to that organization, call [io.kudos.ms.user.core.account.dao.UserOrgUserDao.searchUserIdsByOrgId] directly.
     *
     * @param orgId organization ID
     * @return List<String> list of user IDs; returns an empty list when the organization does not exist or the subtree is empty
     */
    fun getOrgUserIds(orgId: String): List<String>

    /**
     * Gets all direct child organization IDs of the organization by organization ID.
     *
     * @param orgId organization ID
     * @return List<String> list of child organization IDs; returns an empty list if there are no child organizations
     */
    fun getChildOrgIds(orgId: String): List<String>

    /**
     * Gets the user list (including administrators and regular users, deduplicated) under the organization and all
     * its enabled descendant organizations by organization ID.
     * Semantically equivalent to [getOrgUserIds] converted to [UserAccountCacheEntry].
     *
     * @param orgId organization ID
     * @return List<UserAccountCacheEntry> user list
     */
    fun getOrgUsers(orgId: String): List<UserAccountCacheEntry>

    /**
     * Checks whether the user belongs to the specified organization or any of its enabled descendant organizations.
     *
     * That is, "the user is within the orgId subtree". If you only need to determine "the user is directly attached
     * to orgId", call [io.kudos.ms.user.core.account.dao.UserOrgUserDao.exists].
     *
     * @param userId user ID
     * @param orgId organization ID
     * @return true means within the subtree
     */
    fun isUserInOrg(userId: String, orgId: String): Boolean

    /**
     * Gets all direct child organization list of the organization by organization ID.
     *
     * @param orgId organization ID
     * @return List<UserOrgCacheEntry> child organization list; returns an empty list if there are no child organizations
     */
    fun getChildOrgs(orgId: String): List<UserOrgCacheEntry>

    /**
     * Gets the parent organization of the organization by organization ID.
     *
     * @param orgId organization ID
     * @return UserOrgCacheEntry parent organization; returns null if there is no parent organization
     */
    fun getParentOrg(orgId: String): UserOrgCacheEntry?

    /**
     * Gets the organization record by ID (from cache).
     *
     * @param id organization ID
     * @return organization cache entry; returns null if not found
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgRecord(id: String): UserOrgCacheEntry?

    /**
     * Gets the organization list by tenant ID.
     *
     * @param tenantId tenant ID
     * @return list of organization cache entries
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgsByTenantId(tenantId: String): List<UserOrgCacheEntry>

    /**
     * Gets the organization tree structure.
     *
     * @param tenantId tenant ID
     * @param parentId parent organization ID; when null, returns top-level organizations
     * @return list of organization tree nodes (tree structure, containing the children field)
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgTree(tenantId: String, parentId: String? = null): List<UserOrgTreeRow>

    /**
     * Gets all ancestor organization IDs (recursing upward).
     *
     * @param orgId organization ID
     * @return list of ancestor organization IDs (from the direct parent organization to the root organization)
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getAllAncestorOrgIds(orgId: String): List<String>

    /**
     * Gets all descendant organization IDs (recursing downward).
     *
     * @param orgId organization ID
     * @return list of descendant organization IDs (including all child organizations, grandchild organizations, etc.)
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getAllDescendantOrgIds(orgId: String): List<String>

    /**
     * Updates the enabled status of the organization.
     *
     * @param id organization ID
     * @param active whether enabled
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * Moves the organization (adjusts the parent organization and sort number).
     *
     * @param id organization ID
     * @param newParentId new parent organization ID; null means moving to top level
     * @param newSortNum new sort number
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun moveOrg(id: String, newParentId: String?, newSortNum: Int?): Boolean


}

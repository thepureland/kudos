package io.kudos.ms.auth.core.role.datascope.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeExplanationVo
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeVo
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleScope


/**
 * Data-scope (数据权限) business interface: manage roles' custom org grants and resolve a user's
 * effective row-visibility policy across all of their roles.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthRoleDataScopeService : IBaseCrudService<String, AuthRoleScope> {

    /**
     * Update just a role's data-scope policy code (a focused alternative to the full role form).
     * Validates [dataScope] against [io.kudos.ms.auth.common.datascope.enums.DataScopeEnum] and
     * publishes a role-updated event so the role cache (and thus resolution) reflects the change.
     *
     * @param roleId role id
     * @param dataScope the new policy code (ALL / ORG_AND_CHILD / ORG / SELF / CUSTOM); NULL ⇒ ALL
     * @return whether the update succeeded
     * @throws IllegalArgumentException if [dataScope] is non-null/blank and not a recognised code
     */
    fun updateScope(roleId: String, dataScope: String?): Boolean

    /**
     * Returns the org ids granted to a role for CUSTOM data scope.
     *
     * Convenience over [getScopeValues] for the built-in dimension, which is what most callers mean.
     *
     * @param roleId role id
     * @return set of org ids; empty if the role has no custom grants
     */
    fun getOrgIdsByRoleId(roleId: String): Set<String>

    /**
     * Returns the values granted to a role on one scope dimension.
     *
     * @param roleId role id
     * @param dimension the dimension to read, e.g. `org`
     * @return the granted values; empty when the role has no grants there
     */
    fun getScopeValues(roleId: String, dimension: String): Set<String>

    /**
     * Sets a role's grants on one dimension (replace semantics), leaving every other dimension
     * untouched — rebinding a role's orgs must not silently wipe the regions configured elsewhere.
     *
     * @param roleId role id
     * @param dimension the dimension to write
     * @param values the desired complete set for that dimension; empty clears it
     * @return the number of grants persisted
     */
    fun bindScope(roleId: String, dimension: String, values: Collection<String>): Int

    /**
     * Every dimension a role has grants on. For the admin view, which shows the whole picture.
     *
     * @param roleId role id
     * @return dimension -> granted values
     */
    fun getAllScopes(roleId: String): Map<String, Set<String>>

    /**
     * Sets a role's custom data-scope org grants (replace semantics): [orgIds] becomes the
     * complete set, anything previously bound and absent here is removed. An empty collection
     * clears all grants.
     *
     * @param roleId role id
     * @param orgIds the desired complete set of org ids
     * @return the number of grants persisted after the replace
     */
    fun bindOrgs(roleId: String, orgIds: Collection<String>): Int

    /**
     * Resolves a user's effective data scope across all of their (group- and parent-inherited)
     * roles, taking the MOST permissive policy:
     *  - any role with ALL (or an unset/NULL scope) ⇒ [DataScopeVo.all];
     *  - otherwise the union of: the user's own org (ORG), the user's org subtree (ORG_AND_CHILD),
     *    and each CUSTOM role's explicit org grants; SELF contributes the self flag;
     *  - if nothing concrete resolves (e.g. ORG scope but the user has no org) ⇒
     *    [DataScopeVo.selfOnly] (least-surprising restrictive fallback rather than "see nothing").
     *
     * Never throws for a missing/role-less user — returns [DataScopeVo.selfOnly] (most restrictive).
     *
     * **Served from cache.** This sits on the query path — every row-filtered `select` calls it —
     * so it must not do database work per question. See
     * [io.kudos.ms.auth.core.role.datascope.cache.DataScopeByUserIdCache] for what invalidates it
     * and why its staleness is additionally bounded by a TTL.
     *
     * @param userId user id
     * @return the resolved data scope
     */
    fun resolveUserDataScope(userId: String): DataScopeVo

    /**
     * [resolveUserDataScope] without the cache — the resolution itself.
     *
     * Exists so the cache has something to call. Application code should not: bypassing the cache
     * on the query path is exactly the cost this arrangement removes. Use it when you deliberately
     * need a freshly computed answer, such as verifying a configuration change took effect.
     *
     * @param userId user id
     * @return the freshly resolved data scope
     */
    fun computeUserDataScope(userId: String): DataScopeVo

    /**
     * The dimensions this deployment actually has, i.e. the ones something registered a provider
     * for. The admin UI reads it rather than hardcoding a list, so an application that adds a
     * dimension gets a screen for it without touching the console.
     *
     * @return the registered dimension names
     */
    fun getRegisteredDimensions(): List<String>

    /**
     * The same resolution as [resolveUserDataScope], plus the per-role breakdown that explains it.
     *
     * Resolution takes the most permissive policy across every effective role, which makes the
     * outcome impossible to derive by reading one role's configuration — and makes the usual
     * surprise ("I restricted this role and he still sees everything") impossible to diagnose
     * without this.
     *
     * @param userId the subject
     * @return the merged scope, every contributing role, and the role that overrode the rest if any
     */
    fun explainUserDataScope(userId: String): DataScopeExplanationVo

}

package io.kudos.ms.auth.core.role.datascope.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeVo
import io.kudos.ms.auth.core.role.datascope.model.po.AuthRoleOrg


/**
 * Data-scope (数据权限) business interface: manage roles' custom org grants and resolve a user's
 * effective row-visibility policy across all of their roles.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthRoleDataScopeService : IBaseCrudService<String, AuthRoleOrg> {

    /**
     * Returns the org ids granted to a role for CUSTOM data scope.
     *
     * @param roleId role id
     * @return set of org ids; empty if the role has no custom grants
     */
    fun getOrgIdsByRoleId(roleId: String): Set<String>

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
     * @param userId user id
     * @return the resolved data scope
     */
    fun resolveUserDataScope(userId: String): DataScopeVo

}

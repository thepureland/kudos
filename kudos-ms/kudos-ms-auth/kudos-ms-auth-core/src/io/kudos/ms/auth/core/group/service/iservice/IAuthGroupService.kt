package io.kudos.ms.auth.core.group.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.group.vo.response.GroupDeleteImpactVo
import io.kudos.ms.auth.common.role.vo.response.BatchBindResultVo
import io.kudos.ms.auth.core.group.model.po.AuthGroup


/**
 * User group service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IAuthGroupService : IBaseCrudService<String, AuthGroup> {

    /**
     * Aggregate impact summary for deleting a batch of groups: counts of distinct users and
     * roles currently bound to any group in [groupIds]. Used by the admin UI's pre-delete
     * confirmation so operators see the blast radius without spawning 2N GETs.
     *
     * Empty input yields zero counts; missing group ids contribute zero.
     */
    fun getDeleteImpact(groupIds: Collection<String>): GroupDeleteImpactVo

    /**
     * Batch-bind a set of users to a set of groups (Cartesian product). Per-group transaction
     * boundary so a single failure doesn't strand the rest; partial failures are returned in
     * [BatchBindResultVo.failures].
     *
     * Reuses [BatchBindResultVo] from the role package — the payload is owner-agnostic and the
     * UI dialogue is shared between role↔user and group↔user binds.
     */
    fun batchBindUsers(groupIds: Collection<String>, userIds: Collection<String>): BatchBindResultVo

}

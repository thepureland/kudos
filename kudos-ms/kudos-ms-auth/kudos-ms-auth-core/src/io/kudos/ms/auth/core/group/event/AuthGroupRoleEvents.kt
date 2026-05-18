package io.kudos.ms.auth.core.group.event


/**
 * 组-角色关系（`auth_group_role`）领域事件。Join 表，影响以下投影：
 * - RoleIdsByUserIdCache：组下的所有 user 的 "有效角色集合" 都要重算
 * - ResourceIdsByUserIdCache：同上，资源集合都要重算
 *
 * 注意：本事件携带 groupId 而不是 userIds —— listener 需自己用 [io.kudos.ms.auth.core.group.dao.AuthGroupUserDao.searchUserIdsByGroupId]
 * 展开为受影响用户列表。这样设计的原因：发事件的 [io.kudos.ms.auth.core.group.service.impl.AuthGroupRoleService]
 * 只知道 (groupId, roleIds) 维度的变化，不应承担"哪些用户在这个组里"的查询职责。
 *
 * @author K
 * @since 1.0.0
 */
sealed interface AuthGroupRoleEvent

/** 一组组-角色关系变更（绑定或解绑）。 */
data class AuthGroupRoleRelationsChanged(
    val groupId: String,
    val roleIds: Collection<String>,
) : AuthGroupRoleEvent

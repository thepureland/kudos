package io.kudos.ms.auth.common.role.vo.response

import io.kudos.ms.auth.common.group.vo.AuthGroupCacheEntry
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import java.io.Serializable

/**
 * Composite "effective permissions" snapshot for a single user.
 *
 * Replaces the console UI's N+M+K fan-out (`listRoleIdsByUser` + `listGroupIdsByUser` +
 * per-group `listRoleIds` + per-role `listResourceIds` + 3 separate pagingSearch hops for
 * metadata) with one round trip. The fields are flat collections with id keys so the caller
 * can render any view (table grouped by source, tree, etc.) without further server calls.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class EffectivePermissionsVo(
    /**
     * Roles bound directly to the user (NOT including those inherited via groups). Frontend uses
     * this set to mark rows as "Direct" in the source column.
     */
    val directRoles: List<AuthRoleCacheEntry>,

    /**
     * Groups the user belongs to, fully resolved (id + name + tenant + etc). Becomes the second
     * section in the permission viewer.
     */
    val groups: List<AuthGroupCacheEntry>,

    /**
     * groupId → list of roles inherited via that group. A role can appear under multiple groups
     * (and possibly also under [directRoles]); the frontend de-dups by role id and shows all the
     * group sources as tags.
     */
    val rolesByGroup: Map<String, List<AuthRoleCacheEntry>>,

    /**
     * roleId → resources that role grants. A resource can appear under multiple roles; the
     * frontend inverts this map (resource → roles) for the "Granted by" column.
     */
    val resourcesByRole: Map<String, List<SysResourceCacheEntry>>,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun empty(): EffectivePermissionsVo = EffectivePermissionsVo(
            directRoles = emptyList(),
            groups = emptyList(),
            rolesByGroup = emptyMap(),
            resourcesByRole = emptyMap(),
        )
    }
}

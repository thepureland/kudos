package io.kudos.ms.auth.core.role.datascope

import io.kudos.ability.data.rdb.ktorm.rowscope.IRowScopeResolver
import io.kudos.ability.data.rdb.ktorm.rowscope.RowScope
import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import io.kudos.ms.user.common.passport.CurrentUserKit
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * Connects the data layer's row-scope port to this module's resolution.
 *
 * The dependency runs ms → ability, as for the URL enforcement ports: the filter in
 * `kudos-ability-data-rdb-ktorm` never knows that roles or organizations exist, so an application
 * with a different authorization backend swaps this one class.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class RowScopeResolverAdapter : IRowScopeResolver {

    @Resource
    private lateinit var dataScopeService: IAuthRoleDataScopeService

    /**
     * Returns null when nobody is logged in — **not** an unrestricted scope. The distinction is the
     * whole safety property: null means "no subject", which the enforcer turns into a loud failure
     * unless system authority was declared, whereas an unrestricted scope would silently read
     * everything.
     */
    override fun currentScope(): RowScope? {
        val userId = CurrentUserKit.currentUserIdOrNull() ?: return null
        val resolved = dataScopeService.resolveUserDataScope(userId)
        if (resolved.all) return RowScope.unrestricted(userId)
        return RowScope(
            unrestricted = false,
            self = resolved.self,
            principalId = userId,
            // Empty value sets are dropped here so the filter never has to tell "unrestricted"
            // from "restricted to nothing" — by the time it sees the map, an absent dimension is
            // the only way to say "this does not restrict".
            dimensions = resolved.dimensions.filterValues { it.isNotEmpty() },
        )
    }
}

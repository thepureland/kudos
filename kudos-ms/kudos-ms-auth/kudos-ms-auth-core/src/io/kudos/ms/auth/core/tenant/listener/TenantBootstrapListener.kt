package io.kudos.ms.auth.core.tenant.listener

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.tenant.init.properties.TenantBootstrapProperties
import io.kudos.ms.auth.core.tenant.service.iservice.ITenantBootstrapService
import io.kudos.ms.sys.core.tenant.event.SysTenantInserted
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * Seeds a tenant's built-in roles as soon as the tenant exists.
 *
 * Only the roles. Binding an administrator is a separate, explicit call — see
 * [ITenantBootstrapService] — so that "a tenant was created" never silently means "somebody now has
 * administrative access to it".
 *
 * Runs after commit: seeding a tenant that then rolls back would leave orphaned roles carrying a
 * tenant id that never existed.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class TenantBootstrapListener {

    @Resource
    private lateinit var bootstrapService: ITenantBootstrapService

    @Resource
    private lateinit var properties: TenantBootstrapProperties

    private val log = LogFactory.getLog(this::class)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysTenantInserted) {
        if (!properties.enabled) return
        try {
            val result = bootstrapService.seedRoles(event.id)
            if (result.createdRoleCodes.isNotEmpty()) {
                log.info(
                    "Tenant ${event.id} seeded with role(s) ${result.createdRoleCodes.joinToString()} " +
                        "and ${result.createdBindings} permission binding(s).",
                )
            }
        } catch (e: Exception) {
            // The tenant is already committed; failing here must not look like the tenant failed.
            // The state is recoverable — seeding is idempotent, so an operator can simply re-run it.
            log.error(
                "Failed to seed bootstrap roles for tenant ${event.id}; " +
                    "re-run the bootstrap endpoint once the cause is fixed.",
                e,
            )
        }
    }
}

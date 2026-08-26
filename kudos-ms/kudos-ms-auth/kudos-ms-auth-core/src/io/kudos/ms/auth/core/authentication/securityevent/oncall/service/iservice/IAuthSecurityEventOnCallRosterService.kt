package io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice

import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRoster
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterSaveCommand

/** Tenant-scoped on-call rotations: authoritative reads for management, cached reads for delivery. */
interface IAuthSecurityEventOnCallRosterService {

    /** Every rotation of the tenant with its shifts, read straight from the database. */
    fun listByTenant(tenantId: String): List<AuthSecurityEventOnCallRoster>

    /** The tenant's enabled rotation with this code, or `null` when it is absent or disabled. */
    fun findEnabled(tenantId: String, rosterCode: String): AuthSecurityEventOnCallRoster?

    /** Replaces one rotation and its whole shift set under optimistic concurrency control. */
    fun save(command: AuthSecurityEventOnCallRosterSaveCommand): AuthSecurityEventOnCallRoster
}

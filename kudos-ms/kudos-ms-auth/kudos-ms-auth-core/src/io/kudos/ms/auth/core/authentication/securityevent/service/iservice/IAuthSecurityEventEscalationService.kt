package io.kudos.ms.auth.core.authentication.securityevent.service.iservice

interface IAuthSecurityEventEscalationService {
    /** Scans a bounded global batch; safe for concurrent callers because each event is advanced by CAS. */
    fun scanDue(limit: Int = 100): Int
}

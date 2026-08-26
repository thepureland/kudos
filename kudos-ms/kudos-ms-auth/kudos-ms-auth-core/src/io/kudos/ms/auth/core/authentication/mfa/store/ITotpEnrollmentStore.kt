package io.kudos.ms.auth.core.authentication.mfa.store

import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollment

/** Replaceable short-lived store with optimistic update and atomic one-time consumption. */
interface ITotpEnrollmentStore {
    fun create(enrollment: TotpEnrollment): Boolean
    fun get(id: String): TotpEnrollment?
    fun save(enrollment: TotpEnrollment, expectedVersion: Long): TotpEnrollment?
    fun consume(id: String, expectedVersion: Long): TotpEnrollment?
}

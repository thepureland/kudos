package io.kudos.ms.user.core.account.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class DefaultPasswordPolicyTest {

    @Test
    fun defaultPolicyAcceptsLongPassphrasesWithoutCompositionRequirements() {
        val policy = DefaultPasswordPolicy(PasswordPolicyProperties())

        assertTrue(
            policy.violations(
                "four calm words make a password",
                PasswordPolicyContext(PasswordPurpose.LOGIN, username = "alice"),
            ).isEmpty()
        )
    }

    @Test
    fun defaultPolicyRejectsShortCommonRepeatedAndUsernameRelatedValues() {
        val policy = DefaultPasswordPolicy(PasswordPolicyProperties())

        assertEquals(
            setOf(PasswordPolicyViolation.TOO_SHORT, PasswordPolicyViolation.COMMON_PASSWORD),
            policy.violations("password", PasswordPolicyContext(PasswordPurpose.LOGIN, username = "alice")),
        )
        assertTrue(
            PasswordPolicyViolation.REPEATED_CHARACTER in
                policy.violations("aaaaaaaaaaaa", PasswordPolicyContext(PasswordPurpose.LOGIN))
        )
        assertTrue(
            PasswordPolicyViolation.USERNAME_INCLUDED in
                policy.violations("alice has a strong passphrase", PasswordPolicyContext(PasswordPurpose.LOGIN, username = "alice"))
        )
    }

    @Test
    fun compositionRulesAreOptInAndApplicationCanDisableDefaultPolicy() {
        val properties = PasswordPolicyProperties().apply {
            requireUppercase = true
            requireDigit = true
            requireSpecial = true
        }
        val policy = DefaultPasswordPolicy(properties)
        val violations = policy.violations(
            "lowercase words only",
            PasswordPolicyContext(PasswordPurpose.SECURITY),
        )

        assertEquals(
            setOf(
                PasswordPolicyViolation.UPPERCASE_REQUIRED,
                PasswordPolicyViolation.DIGIT_REQUIRED,
                PasswordPolicyViolation.SPECIAL_REQUIRED,
            ),
            violations,
        )

        properties.enabled = false
        assertTrue(policy.violations("x", PasswordPolicyContext(PasswordPurpose.LOGIN)).isEmpty())
    }
}

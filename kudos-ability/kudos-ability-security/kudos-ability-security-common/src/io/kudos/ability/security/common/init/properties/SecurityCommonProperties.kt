package io.kudos.ability.security.common.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Config bindings for `kudos-ability-security-common`.
 *
 * Ported from soul's `SecurityCommonProperties` but trimmed: soul shipped `algTimes` and a
 * generic `keys: Map<String, String>` that nothing in the codebase actually read. Both dropped.
 * Added [Totp] for the one piece of behaviour that's worth tuning per-app (TOTP drift tolerance).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.security.common")
class SecurityCommonProperties {

    /** TOTP-specific knobs (see [Totp]). */
    var totp: Totp = Totp()

    class Totp {
        /**
         * Verification window radius, measured in 30-second TOTP steps. The validator accepts
         * any code from the `[-windowSize, +windowSize]` interval around the current window,
         * so the effective drift tolerance is `windowSize * 30` seconds on either side.
         *
         * Default 1 (±30s). Soul defaulted to 3 (±90s), which is overly permissive for a
         * 30-second TOTP: it widens the brute-force window by 6x. Bump only if you have
         * documented device-clock-skew problems. Max 17 (Google PAM convention).
         */
        var windowSize: Int = 1
    }
}

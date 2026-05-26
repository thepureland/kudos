package io.kudos.ability.comm.email.enums

import io.kudos.base.enums.ienums.IDictEnum


/**
 * Email send result status. Derived from the breakdown of JavaMail `SendFailedException`:
 *  - All messages sent successfully -> [SUCCESS]
 *  - Has `validSentAddresses` **and** has `validUnsentAddresses`/`invalidAddresses` -> [SUCCESS_PART]
 *  - No recipients succeeded -> [FAIL]
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
enum class EmailStatusEnum(
    override val code: String,
    override val displayText: String
) : IDictEnum {

    /** All recipients failed. */
    FAIL("0", "Failed"),

    /** Some recipients succeeded and some failed; the business side typically needs to push the failed list to a retry queue. */
    SUCCESS_PART("1", "Partial success"),

    /** All recipients succeeded. */
    SUCCESS("2", "Success");

}

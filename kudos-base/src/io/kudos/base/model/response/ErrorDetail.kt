package io.kudos.base.model.response

/**
 * Error detail.
 *
 * Carries structured error information for a failed request.
 * Suitable for:
 * 1. Parameter validation failures
 * 2. Field-level error messages
 * 3. Per-item failures of batch operations
 * 4. Complex business validation failures
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
data class ErrorDetail(

    /**
     * Detail error code.
     *
     * Identifies the type of this specific error.
     * Distinct from the outer ApiResponse.code, which represents the overall failure type of the request;
     * this code represents a finer-grained category of a single error.
     *
     * Examples:
     * - REQUIRED
     * - INVALID_FORMAT
     * - VALUE_TOO_LARGE
     * - USERNAME_EXISTS
     *
     * Nullable:
     * - May be omitted when the current scenario only needs message and no finer-grained error categorization is required.
     */
    val code: String? = null,

    /**
     * Name of the field in error.
     *
     * Mainly used for field-level validation failures, allowing the frontend to bind the error to a specific form field.
     *
     * Examples:
     * - name
     * - age
     * - address.city
     * - items[0].price
     *
     * Nullable:
     * - May be omitted when the error is not a field error but pertains to a record, business object, or data row.
     */
    val field: String? = null,

    /**
     * Target of the error.
     *
     * Describes the object, record, row, element, or business target that this error pertains to.
     * When field alone cannot express the error location, target can be used as a supplement.
     *
     * Examples:
     * - row[3]
     * - user:1001
     * - order:20260001
     * - items[2]
     *
     * Nullable:
     * - May be omitted when field already conveys the error location, or when a specific target is unnecessary.
     */
    val target: String? = null,

    /**
     * Error message.
     *
     * Textual description of this specific error.
     * This is the primary message displayed to the frontend or caller.
     *
     * Examples:
     * - Name cannot be empty
     * - Age cannot be less than 0
     * - Invalid phone number format on row 3
     * - Order status does not allow payment
     *
     * Generally not recommended to be empty.
     */
    val message: String,

    /**
     * Rejected value.
     *
     * Records the original input value that caused this error, useful for troubleshooting or frontend display.
     *
     * Examples:
     * - ""                // empty string
     * - -1                // invalid numeric value
     * - "abc@@"           // malformed input
     * - map/list/object   // complex request body fragment
     *
     * Nullable:
     * - May be omitted in scenarios where returning the value is inconvenient or echoing the original value would be unsafe.
     */
    val rejectedValue: Any? = null
)

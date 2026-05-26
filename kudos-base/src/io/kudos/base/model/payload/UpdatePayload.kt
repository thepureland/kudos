package io.kudos.base.model.payload

/**
 * Base class for update-data-item payloads.
 *
 * Note: this class must NOT be exposed to untrusted sources such as the client!
 *
 * Update rules:
 *    1. The `id` property is not updated.
 *    2. All properties listed in `nullProperties` are updated to null.
 *    3. Other properties are updated only when their value is non-null.
 *
 * @author K
 * @since 1.0.0
 */
open class UpdatePayload<S: ISearchPayload> {

    /** List of properties whose value should be set to null. */
    open var nullProperties: List<String>? = null

    /**
     * The query-item payload. If null, the query conditions can also be customized via a where-expression factory
     * function; in that case, conditions are combined with AND.
     */
    open var searchPayload: S? = null

}

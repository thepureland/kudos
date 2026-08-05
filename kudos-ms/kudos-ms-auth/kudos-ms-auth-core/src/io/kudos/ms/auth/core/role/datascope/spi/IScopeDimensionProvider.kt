package io.kudos.ms.auth.core.role.datascope.spi


/**
 * Teaches the data-scope resolver about one dimension.
 *
 * A dimension is a way of slicing rows — organization, region, brand, project. kudos resolves `org`
 * itself because the org tree lives in `kudos-ms-user`; everything else is the application's, and
 * this is how it says so without forking the model.
 *
 * The only thing the resolver needs to know beyond the raw values is **whether they nest**: granting
 * "EMEA" usually implies its countries, and granting an org usually implies its children. A flat
 * dimension answers [expand] with the input unchanged, which is a perfectly good answer and the
 * default.
 *
 * Implementations are Spring beans; contributing one registers the dimension.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IScopeDimensionProvider {

    /**
     * The dimension name, matching `auth_role_scope.dimension`. Must be stable — it is stored in
     * every grant row, so renaming it orphans them.
     */
    fun dimension(): String

    /**
     * Expands values to everything they imply.
     *
     * **Called only where a policy asks for it** — `ORG_AND_CHILD` means "own org and below", so the
     * resolver expands there; `ORG` means own org, and an explicit CUSTOM list means the values an
     * admin actually picked. Expansion is a property of how a value was granted, not of the
     * dimension, so this is never applied uniformly.
     *
     * A flat dimension returns [values] unchanged.
     *
     * Must not throw for unknown values: a value deleted out from under a grant simply contributes
     * nothing, which is recoverable — an exception on the resolution path is not.
     *
     * @param values the granted values
     * @return the closure of those values under the dimension's own hierarchy
     */
    fun expand(values: Set<String>): Set<String> = values

    /**
     * The subject's own position on this dimension, when it has one — the org they belong to, the
     * region they work in. Backs the "own scope and below" policies, which are stated relative to
     * the subject rather than as an explicit list.
     *
     * @param userId the subject
     * @return their value on this dimension, or null when the dimension does not apply to them
     */
    fun valueOfUser(userId: String): String? = null
}

package io.kudos.ms.auth.common.datascope.vo.response

import java.io.Serializable


/**
 * Why a subject can see the rows they can see.
 *
 * The resolved scope alone does not answer the question operators actually ask. Resolution takes the
 * **most permissive** policy across every effective role, so the surprise is nearly always the same
 * one: "I restricted this role, why does he still see everything?" — because some other role of his
 * is ALL. Only the per-role breakdown makes that visible.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class DataScopeExplanationVo(

    /** The subject asked about. */
    val userId: String,

    /** The merged outcome — what the row filter will actually apply. */
    val resolved: DataScopeVo,

    /**
     * Every effective role of the subject and what it contributed. Roles that contribute nothing are
     * listed too: "this role is configured but adds nothing here" is itself an answer, and hiding it
     * would send the reader looking for a role that is in fact irrelevant.
     */
    val contributions: List<RoleContribution>,

    /**
     * Set when one role's ALL policy short-circuits everything else, naming that role. The single
     * most useful sentence this endpoint can produce.
     */
    val overriddenBy: RoleContribution? = null,

) : Serializable {

    /** One role's share of the outcome. */
    data class RoleContribution(

        val roleId: String,
        val roleCode: String?,
        val roleName: String?,

        /** The role's policy code: ALL / ORG_AND_CHILD / ORG / SELF / CUSTOM. */
        val policy: String,

        /** How the subject holds this role: DIRECT / GROUP / INHERITED. */
        val source: String,

        /** dimension → the values this role contributed; empty when it contributed none. */
        val contributed: Map<String, Set<String>> = emptyMap(),

        /** True when this role contributed the SELF flag. */
        val contributesSelf: Boolean = false,

    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

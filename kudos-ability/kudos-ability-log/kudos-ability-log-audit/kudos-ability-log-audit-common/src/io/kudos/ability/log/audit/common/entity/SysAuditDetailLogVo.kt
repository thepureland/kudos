package io.kudos.ability.log.audit.common.entity

import java.io.Serial
import java.io.Serializable

/**
 * Audit detail VO.
 *
 * A single audit log can have multiple details (e.g. one per entity in a batch operation); details
 * carry the concrete request URL, params and description, while [SysAuditLogVo] describes the
 * metadata of the operation as a whole.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class SysAuditDetailLogVo : Serializable {
    /**
     * Primary key
     */
    var id: String? = null

    /**
     * Business entity id (the id of the affected object)
     */
    var auditId: String? = null

    /**
     * Operation URL (full path)
     */
    var operateUrl: String? = null

    /**
     * String params for the description, corresponds to: {0}
     */
    var stringParams: String? = null

    /**
     * Object params for the description, JSON string, corresponds to: ${}
     */
    var objectParams: String? = null

    /**
     * requestReferer
     */
    var requestReferer: String? = null

    /**
     * POST request payload
     */
    var requestFormData: String? = null

    /**
     * Detail description; the converted form of the POST data
     */
    var description: String? = null

    /** Default no-arg constructor for deserialization */
    constructor()

    /**
     * Constructor with primary key (used when assembling a detail manually for query results).
     *
     * @param id detail primary key
     * @author K
     * @since 1.0.0
     */
    constructor(id: String?) {
        this.id = id
    }

    companion object {
        /** Temporary placeholder key for the description field; when present in a business context,
         *  indicates the detail description has not yet been formatted */
        const val AUDIT_LOG_DESC: String = "__AUDIT_LOG_TMP_DESC__"

        /** Serializable version uid */
        @Serial
        private val serialVersionUID = -3787813089272077741L
    }
}

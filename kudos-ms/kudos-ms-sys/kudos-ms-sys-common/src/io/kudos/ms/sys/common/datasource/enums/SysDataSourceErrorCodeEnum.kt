package io.kudos.ms.sys.common.datasource.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Data source error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysDataSourceErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Data source lookup by primary key or dimension failed. */
    DATA_SOURCE_NOT_FOUND("DATA_SOURCE_NOT_FOUND", "Data source does not exist"),

    /** (sub_system_code, micro_service_code, tenant_id) already maps to an existing data source (violates uq_sys_data_source). */
    DATA_SOURCE_ALREADY_EXISTS("DATA_SOURCE_ALREADY_EXISTS", "A data source already exists for this sub-system, microservice, and tenant");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.datasource"

}

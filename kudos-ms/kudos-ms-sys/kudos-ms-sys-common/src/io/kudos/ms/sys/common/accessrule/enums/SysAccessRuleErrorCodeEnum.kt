package io.kudos.ms.sys.common.accessrule.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Access rule error codes.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Cursor
 * @since 1.0.0
 */
enum class SysAccessRuleErrorCodeEnum(
    /** Error code. */
    override val code: String,
    /** Default display text. */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error. */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** When creating an IP rule, parentRuleId is missing and cannot be resolved from the sub-system (systemCode missing). */
    IP_RULE_CREATE_SYSTEM_CODE_REQUIRED(
        "IP_RULE_CREATE_SYSTEM_CODE_REQUIRED",
        "Parent rule not specified when creating IP access rule: please provide parentRuleId, or provide the sub-system code (systemCode) so the server can resolve the parent access rule.",
    ),

    /** No access rule master record found for the given sub-system and tenant. */
    PARENT_ACCESS_RULE_NOT_FOUND(
        "PARENT_ACCESS_RULE_NOT_FOUND",
        "No access rule exists for the current sub-system and tenant; please create an access rule before adding IP ranges.",
    ),

    /** An access rule master record already exists for the same sub-system code and tenant (including platform-level tenant_id null). */
    ACCESS_RULE_ALREADY_EXISTS(
        "ACCESS_RULE_ALREADY_EXISTS",
        "An access rule already exists for this sub-system and tenant; do not create duplicates.",
    ),

    /** The IP start address cannot be parsed into a valid storage value (e.g. IPv4/IPv6 text or in-database integer value). */
    INVALID_IP_START_ADDRESS(
        "INVALID_IP_START_ADDRESS",
        "Invalid IP start address",
    ),

    /** The IP end address cannot be parsed into a valid storage value (e.g. IPv4/IPv6 text or in-database integer value). */
    INVALID_IP_END_ADDRESS(
        "INVALID_IP_END_ADDRESS",
        "Invalid IP end address",
    ),

    /** Access rule lookup by primary key failed. */
    ACCESS_RULE_NOT_FOUND(
        "ACCESS_RULE_NOT_FOUND",
        "Access rule does not exist",
    ),

    /** IP access rule lookup by primary key failed. */
    IP_RULE_NOT_FOUND(
        "IP_RULE_NOT_FOUND",
        "IP access rule does not exist",
    );

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.accessrule"

}

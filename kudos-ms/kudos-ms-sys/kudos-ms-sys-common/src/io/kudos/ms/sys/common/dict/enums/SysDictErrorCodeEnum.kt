package io.kudos.ms.sys.common.dict.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Dictionary module error codes.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysDictErrorCodeEnum(
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Unspecified error */
    UNSPECIFIED("UNSPECIFIED", "Unspecified error"),

    /** Dictionary not found by primary key */
    DICT_NOT_FOUND("DICT_NOT_FOUND", "Dictionary not found"),

    /** A dictionary with the same (atomic_service_code, dict_type) already exists (violates uq_sys_dict) */
    DICT_ALREADY_EXISTS("DICT_ALREADY_EXISTS", "A dictionary of the same type already exists for this atomic service"),

    /** Dictionary item not found by primary key */
    DICT_ITEM_NOT_FOUND("DICT_ITEM_NOT_FOUND", "Dictionary item not found"),

    /** A dictionary item with the same (dict_id, item_code) already exists */
    DICT_ITEM_CODE_ALREADY_EXISTS("DICT_ITEM_CODE_ALREADY_EXISTS", "A dictionary item with the same code already exists in this dictionary");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.dict"

}

package io.kudos.ms.sys.common.dict.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 字典错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysDictErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键查找字典失败 */
    DICT_NOT_FOUND("DICT_NOT_FOUND", "字典不存在"),

    /** (atomic_service_code, dict_type) 已存在对应字典（违反 uq_sys_dict 唯一约束） */
    DICT_ALREADY_EXISTS("DICT_ALREADY_EXISTS", "该原子服务下已存在同类型字典"),

    /** 按主键查找字典项失败 */
    DICT_ITEM_NOT_FOUND("DICT_ITEM_NOT_FOUND", "字典项不存在"),

    /** (dict_id, item_code) 已存在对应字典项 */
    DICT_ITEM_CODE_ALREADY_EXISTS("DICT_ITEM_CODE_ALREADY_EXISTS", "该字典下已存在相同编码的字典项");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.dict"

}

package io.kudos.ms.sys.common.accessrule.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 访问规则错误码
 *
 * @author K
 * @author AI: Codex
 * @author AI: Cursor
 * @since 1.0.0
 */
enum class SysAccessRuleErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 新建 IP 规则时未传 parentRuleId，且无法根据子系统解析（缺少 systemCode） */
    IP_RULE_CREATE_SYSTEM_CODE_REQUIRED(
        "IP_RULE_CREATE_SYSTEM_CODE_REQUIRED",
        "新增 IP 访问规则时未指定父规则：请传入 parentRuleId，或传入子系统编码（systemCode）由服务端解析父访问规则。",
    ),

    /** 按子系统与租户维度未找到访问规则主表记录 */
    PARENT_ACCESS_RULE_NOT_FOUND(
        "PARENT_ACCESS_RULE_NOT_FOUND",
        "在当前子系统与租户下不存在访问规则，请先新增访问规则后再添加 IP 段。",
    ),

    /** 同一子系统编码与租户（含平台级 tenant_id 为空）下已存在访问规则主表记录 */
    ACCESS_RULE_ALREADY_EXISTS(
        "ACCESS_RULE_ALREADY_EXISTS",
        "该子系统与租户下已存在访问规则，请勿重复新增。",
    ),

    /** IP 起始地址无法解析为合法存储值（如 IPv4/IPv6 文本或库内整值） */
    INVALID_IP_START_ADDRESS(
        "INVALID_IP_START_ADDRESS",
        "无效的IP起始地址",
    ),

    /** IP 结束地址无法解析为合法存储值（如 IPv4/IPv6 文本或库内整值） */
    INVALID_IP_END_ADDRESS(
        "INVALID_IP_END_ADDRESS",
        "无效的IP结束地址",
    );

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.accessrule"

}

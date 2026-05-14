package io.kudos.ms.sys.common.datasource.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 数据源错误码
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class SysDataSourceErrorCodeEnum(
    /** 错误码 */
    override val code: String,
    /** 默认展示文本 */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** 未定义错误 */
    UNSPECIFIED("UNSPECIFIED", "未定义错误"),

    /** 按主键或维度查找数据源失败 */
    DATA_SOURCE_NOT_FOUND("DATA_SOURCE_NOT_FOUND", "数据源不存在"),

    /** (sub_system_code, micro_service_code, tenant_id) 已存在对应数据源（违反 uq_sys_data_source） */
    DATA_SOURCE_ALREADY_EXISTS("DATA_SOURCE_ALREADY_EXISTS", "该子系统、微服务、租户维度下已存在数据源");

    override val i18nKeyPrefix: String
        get() = "sys.error-msg.datasource"

}

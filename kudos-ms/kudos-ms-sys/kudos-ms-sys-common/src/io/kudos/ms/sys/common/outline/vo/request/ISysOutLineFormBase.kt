package io.kudos.ms.sys.common.outline.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * 出网白名单表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysOutLineFormBase {

    /** 名称 */
    @get:NotBlank
    @get:MaxLength(64)
    val name: String

    /** 主机名或通配符 */
    @get:NotBlank
    @get:MaxLength(256)
    val host: String

    /** 端口；null 表示任意端口 */
    @get:Min(1)
    @get:Max(65535)
    val port: Int?

    /** 协议(http/https/tcp/any) */
    @get:NotBlank
    @get:MaxLength(16)
    val protocol: String

    /** 系统编码 */
    @get:NotBlank
    @get:MaxLength(32)
    val systemCode: String

    /** 租户id；null 表示平台级 */
    val tenantId: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}

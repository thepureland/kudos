package io.kudos.ms.sys.common.outline.vo.request

/**
 * 出网白名单表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineFormCreate(

    override val name: String,

    override val host: String,

    override val port: Int?,

    override val protocol: String,

    override val systemCode: String,

    override val tenantId: String?,

    override val remark: String?,

) : ISysOutLineFormBase

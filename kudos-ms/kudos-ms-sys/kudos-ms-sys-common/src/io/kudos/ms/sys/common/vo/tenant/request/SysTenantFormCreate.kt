package io.kudos.ms.sys.common.vo.tenant.request


/**
 * 租户表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantFormCreate (

    override val name: String = "",

    override var subSystemCodes: Set<String> = emptySet(),

    override val timezone: String? = null,

    override val defaultLanguageCode: String? = null,

    override val remark: String? = null,

) : ISysTenantFormBase

package io.kudos.ms.sys.common.vo.tenant.request


/**
 * 租户表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantFormCreate (

    override val name: String ,

    override var subSystemCodes: Set<String>,

    override val timezone: String? ,

    override val defaultLanguageCode: String? ,

    override val remark: String? ,

) : ISysTenantFormBase

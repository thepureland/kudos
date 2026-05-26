package io.kudos.ms.sys.common.tenant.vo.request

/**
 * Tenant create form request VO.
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

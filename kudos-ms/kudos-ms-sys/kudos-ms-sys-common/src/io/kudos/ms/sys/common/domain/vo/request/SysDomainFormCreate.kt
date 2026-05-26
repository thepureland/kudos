package io.kudos.ms.sys.common.domain.vo.request

/**
 * Domain create form request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainFormCreate (

    override val domain: String ,

    override val systemCode: String ,

    override val tenantId: String ,

    override val remark: String? ,

) : ISysDomainFormBase

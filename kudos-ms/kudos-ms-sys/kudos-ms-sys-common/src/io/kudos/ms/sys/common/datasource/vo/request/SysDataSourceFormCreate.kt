package io.kudos.ms.sys.common.datasource.vo.request

/**
 * 数据源表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceFormCreate (

    override val name: String ,

    override val subSystemCode: String ,

    override val microServiceCode: String ,

    override val tenantId: String? ,

    override val url: String ,

    override val username: String ,

    override val password: String? ,

    override val initialSize: Int? ,

    override val maxActive: Int? ,

    override val maxIdle: Int? ,

    override val minIdle: Int? ,

    override val maxWait: Int? ,

    override val maxAge: Int? ,

    override val remark: String? ,

) : ISysDataSourceFormBase
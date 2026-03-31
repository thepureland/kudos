package io.kudos.ms.sys.common.vo.datasource.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 数据源表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val name: String = "",

    override val subSystemCode: String = "",

    override val microServiceCode: String = "",

    override val tenantId: String? = null,

    override val url: String = "",

    override val username: String = "",

    override val password: String? = null,

    override val initialSize: Int? = null,

    override val maxActive: Int? = null,

    override val maxIdle: Int? = null,

    override val minIdle: Int? = null,

    override val maxWait: Int? = null,

    override val maxAge: Int? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, ISysDataSourceFormBase
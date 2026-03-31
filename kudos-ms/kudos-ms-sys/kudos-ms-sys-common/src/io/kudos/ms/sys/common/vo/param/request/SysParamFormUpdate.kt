package io.kudos.ms.sys.common.vo.param.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 参数表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val paramName: String = "",

    override val paramValue: String = "",

    override val defaultValue: String? = null,

    override val atomicServiceCode: String = "",

    override val orderNum: Int? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, ISysParamFormBase

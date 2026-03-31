package io.kudos.ms.sys.common.vo.microservice.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 微服务表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceFormUpdate (

    override val code: String = "",

    override val name: String = "",

    override val context: String = "",

    override val atomicService: Boolean = true,

    override val parentCode: String? = null,

    override val remark: String? = null,

) : ISysMicroServiceFormBase, IIdEntity<String> {

    override val id: String
        get() = code

}

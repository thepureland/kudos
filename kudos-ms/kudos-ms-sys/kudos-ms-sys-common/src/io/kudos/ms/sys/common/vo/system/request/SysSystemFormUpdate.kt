package io.kudos.ms.sys.common.vo.system.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 系统表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemFormUpdate (

    override val code: String = "",

    override val name: String = "",

    override val subSystem: Boolean = true,

    override val parentCode: String? = null,

    override val remark: String? = null,

) : ISysSystemFormBase, IIdEntity<String> {

    override val id: String
        get() = code

}

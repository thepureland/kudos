package io.kudos.ms.sys.common.vo.dict.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 字典表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class ISysDictFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val dictType: String = "",

    override val dictName: String = "",

    override val atomicServiceCode: String = "",

    override val remark: String? = null,

) : IIdEntity<String?>, ISysDictFormBase

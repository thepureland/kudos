package io.kudos.ms.sys.common.vo.dict.request


/**
 * 字典表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class ISysDictFormCreate (

    override val dictType: String = "",

    override val dictName: String = "",

    override val atomicServiceCode: String = "",

    override val remark: String? = null,

) : ISysDictFormCommonConstraints

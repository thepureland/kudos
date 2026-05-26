package io.kudos.ms.sys.common.dict.vo.request

/**
 * Dictionary create form request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class ISysDictFormCreate (

    override val dictType: String ,

    override val dictName: String ,

    override val atomicServiceCode: String ,

    override val remark: String? ,

) : ISysDictFormBase

package io.kudos.ms.sys.common.dict.vo.request

/**
 * Dictionary item create form request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemFormCreate (

    override val itemCode: String ,

    override val itemName: String ,

    override val dictId: String ,

    override val orderNum: Int? ,

    override val parentId: String? ,

    override val remark: String? ,

) : ISysDictItemFormBase

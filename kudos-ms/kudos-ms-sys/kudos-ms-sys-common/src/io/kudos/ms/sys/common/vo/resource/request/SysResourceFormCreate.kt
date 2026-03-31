package io.kudos.ms.sys.common.vo.resource.request


/**
 * 资源表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceFormCreate (

    override val name: String = "",

    override val url: String? = null,

    override val resourceTypeDictCode: String = "",

    override val parentId: String? = null,

    override val orderNum: Int? = null,

    override val icon: String? = null,

    override val subSystemCode: String = "",

    override val remark: String? = null,

) : ISysResourceFormBase

package io.kudos.ms.user.common.vo.org.request

/**
 * 机构表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgFormCreate (

    /** 机构名称 */
    val name: String? = null,

    /** 机构简称 */
    val shortName: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 父机构id */
    val parentId: String? = null,

    /** 机构类型字典码 */
    val orgTypeDictCode: String? = null,

    /** 排序号 */
    val sortNum: Int? = null,

    /** 备注 */
    val remark: String? = null,

)

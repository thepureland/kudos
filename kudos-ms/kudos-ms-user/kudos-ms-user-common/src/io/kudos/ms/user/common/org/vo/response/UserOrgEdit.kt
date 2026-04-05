package io.kudos.ms.user.common.org.vo.response
import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 机构编辑响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgEdit (

    /** 主键 */
    override val id: String = "",

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

) : IIdEntity<String>

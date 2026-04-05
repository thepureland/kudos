package io.kudos.ms.user.common.org.vo.request
import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * 机构表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IUserOrgFormBase {

    /** 机构名称 */
    val name: String?

    /** 机构简称 */
    val shortName: String?

    /** 租户id */
    val tenantId: String?

    /** 父机构id */
    val parentId: String?

    /** 机构类型字典码 */
    val orgTypeDictCode: String?

    /** 排序号 */
    val sortNum: Int?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}

package io.kudos.ms.user.common.org.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * Organization form base fields (shared between create and update)
 *
 * @author K
 * @since 1.0.0
 */
interface IUserOrgFormBase {

    val organizationId: String? get() = null
    val nodeKind: String? get() = null

    /** Organization name */
    val name: String?

    /** Organization short name */
    val shortName: String?

    /** Tenant id */
    val tenantId: String?

    /** Parent organization id */
    val parentId: String?

    /** Organization type dict code */
    val orgTypeDictCode: String?

    /** Sort number */
    val sortNum: Int?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}

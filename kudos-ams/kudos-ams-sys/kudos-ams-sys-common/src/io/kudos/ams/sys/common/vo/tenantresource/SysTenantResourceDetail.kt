package io.kudos.ams.sys.common.vo.tenantresource

import io.kudos.base.support.result.IdJsonResult


/**
 * 租户-资源关系查询记录
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysTenantResourceDetail : IdJsonResult<String>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2


    /** 租户id */
    var tenantId: String? = null

    /** 资源id */
    var resourceId: String? = null

}
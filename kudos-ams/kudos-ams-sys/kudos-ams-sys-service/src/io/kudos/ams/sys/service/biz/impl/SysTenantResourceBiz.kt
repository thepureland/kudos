package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysTenantResourceBiz
import io.kudos.ams.sys.service.model.po.SysTenantResource
import io.kudos.ams.sys.service.dao.SysTenantResourceDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 租户-资源关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysTenantResourceBiz : BaseCrudBiz<String, SysTenantResource, SysTenantResourceDao>(), ISysTenantResourceBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
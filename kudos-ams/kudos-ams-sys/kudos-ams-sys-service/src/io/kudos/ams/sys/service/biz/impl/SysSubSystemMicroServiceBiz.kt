package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysSubSystemMicroServiceBiz
import io.kudos.ams.sys.service.model.po.SysSubSystemMicroService
import io.kudos.ams.sys.service.dao.SysSubSystemMicroServiceDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 子系统-微服务关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysSubSystemMicroServiceBiz : BaseCrudBiz<String, SysSubSystemMicroService, SysSubSystemMicroServiceDao>(), ISysSubSystemMicroServiceBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
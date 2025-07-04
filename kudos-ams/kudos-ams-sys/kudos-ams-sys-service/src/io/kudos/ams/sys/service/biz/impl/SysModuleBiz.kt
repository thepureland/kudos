package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysModuleBiz
import io.kudos.ams.sys.service.model.po.SysModule
import io.kudos.ams.sys.service.dao.SysModuleDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 模块业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysModuleBiz : BaseCrudBiz<String, SysModule, SysModuleDao>(), ISysModuleBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
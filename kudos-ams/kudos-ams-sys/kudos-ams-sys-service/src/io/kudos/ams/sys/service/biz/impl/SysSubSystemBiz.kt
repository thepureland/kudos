package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysSubSystemBiz
import io.kudos.ams.sys.service.model.po.SysSubSystem
import io.kudos.ams.sys.service.dao.SysSubSystemDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 子系统业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysSubSystemBiz : BaseCrudBiz<String, SysSubSystem, SysSubSystemDao>(), ISysSubSystemBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysParamBiz
import io.kudos.ams.sys.service.model.po.SysParam
import io.kudos.ams.sys.service.dao.SysParamDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 参数业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysParamBiz : BaseCrudBiz<String, SysParam, SysParamDao>(), ISysParamBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
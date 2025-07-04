package io.kudos.ams.sys.service.biz.impl

import io.kudos.ams.sys.service.biz.ibiz.ISysCacheBiz
import io.kudos.ams.sys.service.model.po.SysCache
import io.kudos.ams.sys.service.dao.SysCacheDao
import io.kudos.ability.data.rdb.ktorm.biz.BaseCrudBiz
import org.springframework.stereotype.Service


/**
 * 缓存业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysCacheBiz : BaseCrudBiz<String, SysCache, SysCacheDao>(), ISysCacheBiz {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
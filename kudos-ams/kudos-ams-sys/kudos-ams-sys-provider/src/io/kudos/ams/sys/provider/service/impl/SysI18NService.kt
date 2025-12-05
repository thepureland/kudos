package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysI18nService
import io.kudos.ams.sys.provider.model.po.SysI18n
import io.kudos.ams.sys.provider.dao.SysI18nDao
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service


/**
 * 国际化业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysI18NService : BaseCrudService<String, SysI18n, SysI18nDao>(), ISysI18nService {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
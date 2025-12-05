package io.kudos.ams.sys.provider.dao

import io.kudos.ability.data.rdb.ktorm.support.ColumnHelper
import io.kudos.ams.sys.common.vo.dict.SysDictRecord
import io.kudos.ams.sys.common.vo.dict.SysDictSearchPayload
import io.kudos.ams.sys.provider.model.table.SysDictItems
import io.kudos.base.error.ObjectNotFoundException
import io.kudos.base.query.enums.OperatorEnum
import org.ktorm.dsl.*
import org.ktorm.expression.OrderByExpression
import io.kudos.ams.sys.provider.model.po.SysDict
import io.kudos.ams.sys.provider.model.table.SysDicts
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 字典数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysDictDao : BaseCrudDao<String, SysDict, SysDicts>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

}
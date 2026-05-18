package io.kudos.ms.sys.core.outline.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import io.kudos.ms.sys.core.outline.model.table.SysOutLines
import org.springframework.stereotype.Repository


/**
 * 出网白名单数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysOutLineDao : BaseCrudDao<String, SysOutLine, SysOutLines>()

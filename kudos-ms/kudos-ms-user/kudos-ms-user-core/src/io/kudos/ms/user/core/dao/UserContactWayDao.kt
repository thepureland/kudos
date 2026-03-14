package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.user.core.model.po.UserContactWay
import io.kudos.ms.user.core.model.table.UserContactWays
import org.springframework.stereotype.Repository


/**
 * 用户联系方式数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class UserContactWayDao : BaseCrudDao<String, UserContactWay, UserContactWays>() {



}

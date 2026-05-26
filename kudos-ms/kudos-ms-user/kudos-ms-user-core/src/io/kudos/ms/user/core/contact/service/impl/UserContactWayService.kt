package io.kudos.ms.user.core.contact.service.impl

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.user.core.contact.dao.UserContactWayDao
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * User contact way service
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class UserContactWayService(
    dao: UserContactWayDao
) : BaseCrudService<String, UserContactWay, UserContactWayDao>(dao), IUserContactWayService {

    @Transactional(readOnly = true)
    override fun getActiveContactValuesByUserIds(
        userIds: Collection<String>,
        contactWayDictCode: String,
    ): Map<String, String> {
        if (userIds.isEmpty()) return emptyMap()
        val criteria = Criteria(UserContactWay::userId inList userIds.toList())
            .addAnd(UserContactWay::contactWayDictCode eq contactWayDictCode)
            .addAnd(UserContactWay::active eq true)
        val rows = dao.search(criteria)
        // For multiple rows with the same userId, pick the first by priority ASC; null priority is treated as lowest priority
        return rows
            .groupBy { it.userId }
            .mapValues { (_, list) ->
                list.minByOrNull { it.priority ?: Int.MAX_VALUE }!!.contactWayValue
            }
    }
}

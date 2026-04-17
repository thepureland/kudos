package io.kudos.ms.user.core.contact.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.user.core.contact.dao.UserContactWayDao
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 用户联系方式业务
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



}

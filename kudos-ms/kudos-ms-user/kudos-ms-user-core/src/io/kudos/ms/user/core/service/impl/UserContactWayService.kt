package io.kudos.ms.user.core.service.impl

import io.kudos.base.support.service.BaseCrudService
import io.kudos.ms.user.core.dao.UserContactWayDao
import io.kudos.ms.user.core.model.po.UserContactWay
import io.kudos.ms.user.core.service.iservice.IUserContactWayService
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

package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupInserted
import io.kudos.ms.auth.core.group.event.AuthGroupUpdated
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 用户组业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthGroupService(
    dao: AuthGroupDao
) : BaseCrudService<String, AuthGroup, AuthGroupDao>(dao), IAuthGroupService {


    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的用户组。")
        eventPublisher.publishEvent(AuthGroupInserted(id))
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, AuthGroup::id.name) as String
        if (success) {
            log.debug("更新id为${id}的用户组。")
            eventPublisher.publishEvent(AuthGroupUpdated(id))
        } else {
            log.error("更新id为${id}的用户组失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val group = dao.get(id) ?: return run {
            log.warn("删除id为${id}的用户组时，发现其已不存在！")
            false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的用户组。")
            eventPublisher.publishEvent(AuthGroupDeleted(id, group.tenantId, group.code))
        } else {
            log.warn("删除id为${id}的用户组失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        // 先 snapshot tenantId/code，AFTER_COMMIT 后下游 (tenantId, code) 缓存无法回查
        val snapshots = if (ids.isNotEmpty()) {
            dao.getByIds(ids).map {
                AuthGroupBatchDeleted.Item(it.id, it.tenantId, it.code)
            }
        } else emptyList()
        val count = super.batchDelete(ids)
        log.debug("批量删除用户组，期望删除${ids.size}条，实际删除${count}条。")
        if (snapshots.isNotEmpty()) {
            eventPublisher.publishEvent(AuthGroupBatchDeleted(snapshots))
        }
        return count
    }


}

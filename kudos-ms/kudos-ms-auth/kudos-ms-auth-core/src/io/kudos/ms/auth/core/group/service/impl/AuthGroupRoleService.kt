package io.kudos.ms.auth.core.group.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 组-角色关系业务
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthGroupRoleService(
    dao: AuthGroupRoleDao
) : BaseCrudService<String, AuthGroupRole, AuthGroupRoleDao>(dao),
    IAuthGroupRoleService {


    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getRoleIdsByGroupId(groupId: String): Set<String> =
        dao.searchRoleIdsByGroupId(groupId)

    @Transactional(readOnly = true)
    override fun getGroupIdsByRoleId(roleId: String): Set<String> =
        dao.searchGroupIdsByRoleId(roleId)

    @Transactional
    override fun batchBind(groupId: String, roleIds: Collection<String>): Int {
        if (roleIds.isEmpty()) return 0
        // 一次 SELECT 已存在的关系，差集对新增 ID 一次 batchInsert，把原 N+1 折叠到 2 次 SQL。
        val existing = dao.searchRoleIdsByGroupId(groupId)
        val newRoleIds = roleIds.toSet() - existing
        if (newRoleIds.isEmpty()) {
            log.debug("批量绑定组${groupId}与${roleIds.size}个角色的关系，全部已存在，无新增。")
            return 0
        }
        val relations = newRoleIds.map { roleId ->
            AuthGroupRole {
                this.groupId = groupId
                this.roleId = roleId
            }
        }
        dao.batchInsert(relations)
        log.debug("批量绑定组${groupId}与${roleIds.size}个角色的关系，成功绑定${newRoleIds.size}条。")
        eventPublisher.publishEvent(AuthGroupRoleRelationsChanged(groupId, newRoleIds.toList()))
        return newRoleIds.size
    }

    @Transactional
    override fun unbind(groupId: String, roleId: String): Boolean {
        val count = dao.deleteByGroupIdAndRoleId(groupId, roleId)
        val success = count > 0
        if (success) {
            log.debug("解绑组${groupId}与角色${roleId}的关系。")
            eventPublisher.publishEvent(AuthGroupRoleRelationsChanged(groupId, listOf(roleId)))
        } else {
            log.warn("解绑组${groupId}与角色${roleId}的关系失败，关系不存在。")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(groupId: String, roleId: String): Boolean = dao.exists(groupId, roleId)


}

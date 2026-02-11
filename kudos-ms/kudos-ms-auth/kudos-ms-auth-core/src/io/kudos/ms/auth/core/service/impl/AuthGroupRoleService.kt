package io.kudos.ms.auth.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.model.po.AuthGroupRole
import io.kudos.ms.auth.core.service.iservice.IAuthGroupRoleService
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
//region your codes 1
open class AuthGroupRoleService : BaseCrudService<String, AuthGroupRole, AuthGroupRoleDao>(),
    IAuthGroupRoleService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getRoleIdsByGroupId(groupId: String): Set<String> {
        return dao.searchRoleIdsByGroupId(groupId)
    }

    override fun getGroupIdsByRoleId(roleId: String): Set<String> {
        return dao.searchGroupIdsByRoleId(roleId)
    }

    @Transactional
    override fun batchBind(groupId: String, roleIds: Collection<String>): Int {
        if (roleIds.isEmpty()) {
            return 0
        }
        var count = 0
        roleIds.forEach { roleId ->
            if (!exists(groupId, roleId)) {
                val relation = AuthGroupRole.Companion {
                    this.groupId = groupId
                    this.roleId = roleId
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定组${groupId}与${roleIds.size}个角色的关系，成功绑定${count}条。")
        return count
    }

    @Transactional
    override fun unbind(groupId: String, roleId: String): Boolean {
        val count = dao.deleteByGroupIdAndRoleId(groupId, roleId)
        val success = count > 0
        if (success) {
            log.debug("解绑组${groupId}与角色${roleId}的关系。")
        } else {
            log.warn("解绑组${groupId}与角色${roleId}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(groupId: String, roleId: String): Boolean {
        return dao.exists(groupId, roleId)
    }

    //endregion your codes 2

}

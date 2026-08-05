package io.kudos.ms.auth.core.role.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.common.role.vo.PermissionBindingRequest
import io.kudos.ms.auth.common.role.vo.PermissionBindingVo
import io.kudos.ms.auth.core.platform.cache.ResourceIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.event.AuthRoleResourceRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleResourceService
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Role-Resource relation business
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional
open class AuthRoleResourceService(
    dao: AuthRoleResourceDao
) : BaseCrudService<String, AuthRoleResource, AuthRoleResourceDao>(dao),
    IAuthRoleResourceService {


    @Autowired
    private lateinit var resourceIdsByRoleIdCache: ResourceIdsByRoleIdCache

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Resource
    private lateinit var grantPolicyService: IAuthGrantPolicyService

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getResourceIdsByRoleId(roleId: String): Set<String> =
        resourceIdsByRoleIdCache.getResourceIds(roleId).toSet()

    @Transactional(readOnly = true)
    override fun getRoleIdsByResourceId(resourceId: String): Set<String> =
        dao.searchRoleIdsByResourceId(resourceId)

    @Transactional
    override fun batchBind(roleId: String, resourceIds: Collection<String>): Int {
        if (resourceIds.isEmpty()) return 0
        // SELECT existing relations once (resource_id is character(N); DB may return strings with padding, trim uniformly).
        val existing = dao.searchResourceIdsByRoleIds(listOf(roleId)).mapTo(mutableSetOf()) { it.trim() }
        val boundResourceIds = resourceIds.mapTo(mutableSetOf()) { it.trim() } - existing
        if (boundResourceIds.isEmpty()) {
            log.debug("Batch binding relations between role ${roleId} and ${resourceIds.size} resources; all already exist, nothing added.")
            return 0
        }

        // Widening a role widens every holder of it and of its descendants, so the addressed
        // permissions have to exist and belong to this role's subsystem. Bare ids used to be
        // inserted unchecked, which let a dangling or foreign resource id into the grant table.
        val reasons = grantPolicyService.screenPermissionBindings(roleId, resourceIds = boundResourceIds)
        require(reasons.isEmpty()) {
            "Grant policy rejected ${reasons.size} permission binding(s) for role ${roleId}:\n" +
                reasons.joinToString("\n") { "  - ${it}" }
        }
        val relations = boundResourceIds.map { trimmed ->
            AuthRoleResource {
                this.roleId = roleId
                this.resourceId = trimmed
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch binding relations between role ${roleId} and ${resourceIds.size} resources; successfully bound ${boundResourceIds.size} entries.")
        eventPublisher.publishEvent(AuthRoleResourceRelationsChanged(roleId, boundResourceIds.toList()))
        return boundResourceIds.size
    }

    @Transactional
    override fun unbind(roleId: String, resourceId: String): Boolean {
        val trimmed = resourceId.trim()
        val count = dao.deleteByRoleIdAndResourceId(roleId, trimmed)
        val success = count > 0
        if (success) {
            log.debug("Unbinding relation between role ${roleId} and resource ${resourceId}.")
            eventPublisher.publishEvent(AuthRoleResourceRelationsChanged(roleId, listOf(trimmed)))
        } else {
            log.warn("Failed to unbind relation between role ${roleId} and resource ${resourceId}; relation does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(roleId: String, resourceId: String): Boolean =
        dao.exists(roleId, resourceId.trim())

    @Transactional(readOnly = true)
    override fun listPermissionBindings(roleId: String): List<PermissionBindingVo> =
        dao.searchBindingsByRoleIds(listOf(roleId))
            .filter { !it.permissionCode.isNullOrBlank() }
            .map { binding ->
                PermissionBindingVo(
                    id = binding.id,
                    roleId = binding.roleId,
                    permissionCode = binding.permissionCode!!.trim(),
                    effect = PermissionEffect.fromCode(binding.effect).name,
                    condition = binding.condition?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
            .sortedBy { it.permissionCode }

    @Transactional
    override fun savePermissionBinding(request: PermissionBindingRequest): String {
        val code = request.permissionCode.trim()
        require(code.isNotEmpty()) { "a permission binding must name its code." }
        val reasons = grantPolicyService.screenPermissionBindings(request.roleId, permissionCodes = listOf(code))
        require(reasons.isEmpty()) {
            "Grant policy rejected this binding:\n" + reasons.joinToString("\n") { "  - ${it}" }
        }
        // Unrecognised effects normalise to ALLOW rather than being guessed at — but a typo that
        // silently became ALLOW when DENY was meant would be the dangerous direction, so the caller
        // is validated instead of trusted.
        require(PermissionEffect.entries.any { it.name.equals(request.effect.trim(), ignoreCase = true) }) {
            "Unknown effect '${request.effect}'; expected ALLOW or DENY."
        }
        val effect = PermissionEffect.fromCode(request.effect).name
        val condition = request.condition?.trim()?.takeIf { it.isNotEmpty() }

        val existing = dao.searchBindingsByRoleIds(listOf(request.roleId))
            .firstOrNull { it.permissionCode?.trim() == code }
        val id = if (existing != null) {
            existing.effect = effect
            existing.condition = condition
            dao.update(existing)
            existing.id
        } else {
            dao.insert(
                AuthRoleResource {
                    this.roleId = request.roleId
                    this.permissionCode = code
                    this.effect = effect
                    this.condition = condition
                },
            )
        }
        log.debug("Saved permission binding ${code} (${effect}) on role ${request.roleId}.")
        eventPublisher.publishEvent(AuthRoleResourceRelationsChanged(request.roleId, listOf(code)))
        return id
    }

    @Transactional
    override fun removePermissionBinding(bindingId: String): Boolean {
        val binding = dao.get(bindingId) ?: return false
        val success = dao.deleteById(bindingId)
        if (success) {
            log.debug("Removed permission binding ${bindingId} from role ${binding.roleId}.")
            eventPublisher.publishEvent(
                AuthRoleResourceRelationsChanged(binding.roleId, listOfNotNull(binding.permissionCode)),
            )
        }
        return success
    }

}

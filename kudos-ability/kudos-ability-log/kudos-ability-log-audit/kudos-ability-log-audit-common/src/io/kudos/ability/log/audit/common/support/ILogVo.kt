package io.kudos.ability.log.audit.common.support

import java.io.Serializable

/**
 * Marker interface for audit log VOs.
 *
 * Used primarily to enforce a unified base-class constraint during cross-process /
 * MQ serialization (requiring [Serializable]), and to constrain generics via this
 * interface. No methods — type marker only.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface ILogVo : Serializable
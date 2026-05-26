package io.kudos.ability.log.audit.common.support

/**
 * Audit-log module-name resolution interface: translates (subsysCode, moduleCode)
 * into (module id, module name) for the audit-log detail page to show "which
 * module's operation".
 *
 * Implementations typically look up a dictionary service / subsystem config and
 * cache hits to avoid hitting the DB for every audit log.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface ISysAuditModule {
    /**
     * Returns the module name.
     *
     * @param subsysCode subsystem code
     * @param moduleCode module code
     * @return Pair<module id, module name>
     */
    fun module(subsysCode: String?, moduleCode: String?): Pair<Int?, String?>
}

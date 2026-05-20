package io.kudos.ability.log.audit.common.support

/**
 * 审计日志模块名解析接口：把 (subsysCode, moduleCode) 翻译成 (模块 id, 模块名)，
 * 供审计日志详情页展示"哪个模块的操作"。
 *
 * 实现方通常从字典服务 / 子系统配置查表，缓存命中后避免每次审计日志都走 DB。
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAuditModule {
    /**
     * 获取module的名称
     *
     * @param subsysCode 子系统编号
     * @param moduleCode 模块编号
     * @return Pair<模块id></模块id>, 模块名>
     */
    fun module(subsysCode: String?, moduleCode: String?): Pair<Int?, String?>
}

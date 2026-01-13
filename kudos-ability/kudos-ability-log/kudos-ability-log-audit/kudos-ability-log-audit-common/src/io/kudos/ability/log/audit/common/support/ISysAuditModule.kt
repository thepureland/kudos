package io.kudos.ability.log.audit.common.support

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

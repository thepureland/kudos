package io.kudos.ability.log.audit.common.support

import java.io.Serializable

/**
 * 审计日志 VO 的标记接口。
 *
 * 主要用于在跨进程 / MQ 序列化时统一基类约束（强制实现 [Serializable]），并便于通过该接口做泛型约束。
 * 没有方法 —— 仅作类型标记。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface ILogVo : Serializable
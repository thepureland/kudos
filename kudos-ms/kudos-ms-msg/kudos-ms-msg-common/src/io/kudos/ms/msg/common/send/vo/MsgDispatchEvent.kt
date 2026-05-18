package io.kudos.ms.msg.common.send.vo

import java.io.Serializable


/**
 * 投递给单个渠道 listener 的事件 payload。
 *
 * 设计取舍：
 * - **不传 templateId**：模板已在 publish service 渲染过，listener 只看 [renderedTitle] / [renderedContent]。
 *   这样模板被改动也不影响在飞消息，调试线索更稳定。
 * - **receiverIds 一次给全**：当前实现按"每条 publish 一个事件"投递，所有接收者打成一包；listener
 *   端再决定要不要按渠道做内部分页（Email 默认整批 SMTP，Site 按用户落库）。
 * - **tenantId 必带**：listener 需要按租户做凭证查询、限流隔离。
 *
 * @author K
 * @since 1.0.0
 */
data class MsgDispatchEvent(

    /** [io.kudos.ms.msg.core.send.model.po.MsgSend] 主键，用于回写计数与最终状态 */
    val sendId: String,

    /** [io.kudos.ms.msg.core.instance.model.po.MsgInstance] 主键，用于审计 */
    val instanceId: String,

    /** 发送渠道字典码：[io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum.dictCode] */
    val publishMethodDictCode: String,

    /** 接收者用户 id 集合 —— 仅 user-级别派发；按租户/角色派发应该在 publish 时已展开 */
    val receiverIds: Set<String>,

    /** 渲染后的标题，已替换占位符 */
    val renderedTitle: String,

    /** 渲染后的正文，已替换占位符 */
    val renderedContent: String,

    /** 语言字典码；listener 可据此挑 SMTP/SMS 模板的 locale 变体 */
    val localeDictCode: String?,

    /** 租户 id（必带） */
    val tenantId: String,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}

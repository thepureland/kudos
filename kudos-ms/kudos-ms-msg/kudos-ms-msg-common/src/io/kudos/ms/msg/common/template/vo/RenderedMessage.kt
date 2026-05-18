package io.kudos.ms.msg.common.template.vo

import java.io.Serializable


/**
 * 模板渲染后产生的"可发送"消息。
 *
 * 与 [MsgTemplateCacheEntry] 的关系：模板里 title/content 是带 `${param}` 占位符的源；
 * 本 VO 是占位符已替换、可直接塞进 SMTP / SMS / push payload 的成品。
 *
 * @author K
 * @since 1.0.0
 */
data class RenderedMessage(

    /** 渲染后的标题；模板和默认标题都为空时为空字符串 */
    val title: String,

    /** 渲染后的正文；模板和默认正文都为空时为空字符串 */
    val content: String,

    /** 渲染时实际用到的参数 map，便于排查/审计哪些占位符被替换了 */
    val paramsUsed: Map<String, String>,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}

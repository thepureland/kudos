package io.kudos.ms.msg.core.template.render

import io.kudos.base.lang.string.fillTemplateByObjectMap
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.common.template.vo.RenderedMessage
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * 把 [MsgTemplateCacheEntry] + 业务参数 渲染成 [RenderedMessage]。
 *
 * 占位符语法沿用 [io.kudos.base.lang.string.fillTemplateByObjectMap] 的 `${name}`。
 * 渲染流程：
 *   1. title/content 任一为空时回退到 defaultTitle / defaultContent
 *   2. 把 [autoParams] 注入到参数 map（业务方传入的同名参数优先级更高，不被覆盖）
 *   3. 用合并后的 map 调 fillTemplateByObjectMap 替换占位符
 *
 * 没有占位符的纯文本模板照样能跑（fillTemplate 找不到 `${name}` 就原样返回）。
 *
 * 自动参数集合 [autoParams]:
 *   - `time`     — yyyy-MM-dd HH:mm:ss
 *   - `date`     — yyyy-MM-dd
 *   - `year` / `month` / `day` — 数字字符串
 *
 * 单元测试通过覆盖 [render] 的 `nowProvider` 参数注入固定时钟；生产路径走默认的
 * `LocalDateTime.now()`。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class MsgTemplateRenderer {

    /**
     * @param nowProvider 用于自动参数 (time/date/year/month/day) 的"当前时刻"。
     *   仅测试需要覆盖；生产留默认即可。
     */
    fun render(
        template: MsgTemplateCacheEntry,
        params: Map<String, Any> = emptyMap(),
        nowProvider: () -> LocalDateTime = LocalDateTime::now,
    ): RenderedMessage {
        val titleSrc = template.title?.takeIf { it.isNotBlank() } ?: template.defaultTitle.orEmpty()
        val contentSrc = template.content?.takeIf { it.isNotBlank() } ?: template.defaultContent.orEmpty()

        // 业务参数同名时优先生效，自动参数仅在缺失时补
        val merged = buildMap<String, Any> {
            putAll(autoParams(nowProvider()))
            putAll(params)
        }

        val title = titleSrc.fillTemplateByObjectMap(merged).toString()
        val content = contentSrc.fillTemplateByObjectMap(merged).toString()
        return RenderedMessage(
            title = title,
            content = content,
            paramsUsed = merged.mapValues { it.value.toString() },
        )
    }

    private fun autoParams(now: LocalDateTime): Map<String, Any> {
        return mapOf(
            "time" to now.format(TIME_FMT),
            "date" to now.toLocalDate().format(DATE_FMT),
            "year" to now.year.toString(),
            "month" to now.monthValue.toString().padStart(2, '0'),
            "day" to now.dayOfMonth.toString().padStart(2, '0'),
        )
    }

    companion object {
        private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}

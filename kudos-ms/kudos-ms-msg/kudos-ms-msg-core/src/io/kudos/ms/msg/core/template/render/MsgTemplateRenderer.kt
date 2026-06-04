package io.kudos.ms.msg.core.template.render

import io.kudos.base.lang.string.fillTemplateByObjectMap
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.common.template.vo.RenderedMessage
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Renders a [MsgTemplateCacheEntry] + business parameters into a [RenderedMessage].
 *
 * Placeholder syntax follows [io.kudos.base.lang.string.fillTemplateByObjectMap]'s `${name}`.
 * Render flow:
 *   1. When either title/content is empty, fall back to defaultTitle / defaultContent.
 *   2. Inject [autoParams] into the parameter map (business-supplied parameters with the same name take precedence and are not overridden).
 *   3. Use the merged map to call fillTemplateByObjectMap to substitute placeholders.
 *
 * Plain-text templates without placeholders work too (fillTemplate returns the input unchanged if no `${name}` is found).
 *
 * Auto parameter set [autoParams]:
 *   - `time`     - yyyy-MM-dd HH:mm:ss
 *   - `date`     - yyyy-MM-dd
 *   - `year` / `month` / `day` - numeric strings
 *
 * Unit tests inject a fixed clock by overriding the `nowProvider` parameter of [render];
 * the production path uses the default `LocalDateTime.now()`.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class MsgTemplateRenderer {

    /**
     * @param nowProvider the "current time" used for auto parameters (time/date/year/month/day).
     *   Only tests need to override this; production can use the default.
     */
    fun render(
        template: MsgTemplateCacheEntry,
        params: Map<String, Any> = emptyMap(),
        nowProvider: () -> LocalDateTime = LocalDateTime::now,
    ): RenderedMessage {
        val titleSrc = template.title?.takeIf { it.isNotBlank() } ?: template.defaultTitle.orEmpty()
        val contentSrc = template.content?.takeIf { it.isNotBlank() } ?: template.defaultContent.orEmpty()

        // Business parameters take precedence on name conflicts; auto parameters only fill in when missing
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

    /**
     * 自动注入到模板上下文的时间相关变量：业务模板写 `{time}` / `{date}` / `{year}` 等即可使用。
     * 月日 padStart 到 2 位（'09' 而非 '9'），避免业务侧需要再做 zero-pad。
     *
     * @param now 当前时间
     * @return time/date/year/month/day 五个变量的 map
     * @author K
     * @since 1.0.0
     */
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

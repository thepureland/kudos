package io.kudos.tools.codegen.core.merge

/**
 * 拼接代码抓取器
 *
 * @author K
 * @since 1.0.0
 */
class AppendCodesRetriever(private val fileContent: CharSequence) {

    fun retrieve(): Map<Int, Pair<AppendCodeType, String>> =
        APPEND_REGEX.findAll(fileContent).associate { match ->
            match.groupValues[3].toInt() to (AppendCodeType.valueOf(match.groupValues[2]) to match.value)
        }

    private companion object {
        private val APPEND_REGEX =
            Regex("(?<=(<!--)?#?//region append (\\w{1,10}) codes (\\d)(-->)?\\r?\\n)[\\s\\S]*?(?=(<!--)?#?//endregion append \\w+ codes \\d(-->)?)")
    }
}

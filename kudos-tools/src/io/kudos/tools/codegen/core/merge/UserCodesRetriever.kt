package io.kudos.tools.codegen.core.merge

/**
 * 用户自定义代码抓取器
 *
 * @author K
 * @since 1.0.0
 */
class UserCodesRetriever(private val fileContent: String) {
    fun retrieve(): Map<Int, String> =
        USER_CODES_REGEX.findAll(fileContent).associate { match ->
            match.groupValues[2].toInt() to match.value
        }

    private companion object {
        private val USER_CODES_REGEX =
            Regex("(?<=(<!--)?#?//region your codes (\\d)(-->)?\\r?\\n)[\\s\\S]*?(?=(<!--)?#?//endregion your codes \\d(-->)?)")
    }
}

package io.kudos.tools.codegen.core.merge

import io.kudos.base.io.FileKit
import java.io.File
import java.util.regex.Matcher

/**
 * 代码合并器，用于合并历史已生成的代码和当前生成的代码
 *
 * @author K
 * @since 1.0.0
 */
class CodeMerger(private val file: File) {

    /** 被覆盖前的旧文件内容，用作"已生成代码"基线，从中提取用户自填代码与历史 import */
    private val oldFileContent: String = FileKit.readFileToString(file)
    /** 合并过程中持续累积的新文件内容；写盘前由 [merge] 最后调用 [FileKit.write] 落盘 */
    private lateinit var newFileContent: CharSequence
    /** 用户自填代码（`//region your codes XXX` 区块内）的解析器 */
    private val retriever: UserCodesRetriever = UserCodesRetriever(oldFileContent)

    /**
     * 代码生成后进行合并
     */
    fun merge() {
        handleRegion()
        handleImport()
        FileKit.write(file, newFileContent)
    }

    /**
     * 合并 `//region your codes N` ... `//endregion your codes N` 之间的用户自填代码。
     *
     * 流程：
     * 1. 从旧文件提取每个 region 内的用户代码（key = region 序号）；
     * 2. 读取新生成的文件作为初稿；
     * 3. 如果模板里通过 `AppendCodesRetriever` 声明了"要追加到该 region 的代码"，按 [AppendCodeType] 决定是整段追加还是按行追加（PARTIBLE 模式下逐行去重）；
     * 4. 用正则把每段 region 内容替换为「用户代码 + 追加代码」；正则兼容 HTML/JSP 注释 `<!--//region-->` 风格。
     *
     * @author K
     * @since 1.0.0
     */
    private fun handleRegion() {
        val customCodes = retriever.retrieve()
        newFileContent = FileKit.readFileToString(file)
        val appendCodesMap = AppendCodesRetriever(newFileContent).retrieve()
        for ((index, value) in customCodes) {
            val codes = StringBuilder(value)
            appendCodesMap[index]?.let { (type, appendCodes) ->
                if (type == AppendCodeType.PARTIBLE) {
                    appendCodes.split("\n")
                        .filter { codes.indexOf(it) == -1 }
                        .forEach { codes.append(it).append("\n") }
                    codes.append("\n")
                } else if (codes.indexOf(appendCodes) == -1) {
                    codes.append(appendCodes).append("\n")
                }
            }
            val regexp =
                "(?<=(<!--)?#?//region your codes $index(-->)?\\r?\\n)[\\s\\S]*?(?=(<!--)?#?//endregion your codes $index(-->)?)"
            newFileContent = newFileContent.replaceFirst(regexp.toRegex(), Matcher.quoteReplacement(codes.toString()))
        }
    }

    /**
     * 合并 import 语句（仅对 `.kt` 文件生效）。
     *
     * 旧 import 减去与新 import 的交集 = 用户自己加的 import。把这部分插回新文件第一个 `import` 之前，
     * 既能保留用户引入的额外依赖，又不会重复模板中已有的 import。
     *
     * @author K
     * @since 1.0.0
     */
    private fun handleImport() {
        if (!file.name.endsWith(".kt")) return
        val oldImports = ImportStmtRetriever(oldFileContent).retrieveImports()
        val newImports = ImportStmtRetriever(newFileContent).retrieveImports()
        // 差集即用户自己导入的 import
        val customImport = oldImports.subtract(newImports.toSet())
        if (customImport.isEmpty()) return
        val imports = customImport.joinToString(separator = "", postfix = "") { "$it\n" }
        val index = newFileContent.indexOf("import")
        newFileContent = StringBuilder(newFileContent).insert(index, imports).toString()
    }
}
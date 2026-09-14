package io.kudos.ms.tag.core.runtime.rdb

import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.query.model.TagQueryExpression.AllTags
import io.kudos.ms.tag.common.query.model.TagQueryExpression.AnyTags
import io.kudos.ms.tag.common.query.model.TagQueryExpression.NotTags
import io.kudos.ms.tag.core.runtime.port.TagKeysetPage
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class CompiledTagAssignmentQuery(
    val sql: String,
    val parameters: List<Any>,
)

/** Compiles the public tag-only Boolean expression into portable parameterized SQL. */
class TagAssignmentQueryCompiler(
    private val groupedAllTagsThreshold: Int = 4,
) {
    fun compile(
        tenantId: String,
        subjectType: String,
        expression: TagQueryExpression,
        tagIdsByCode: Map<String, String>,
        page: TagKeysetPage,
        now: Instant = Instant.now(),
    ): CompiledTagAssignmentQuery {
        var aliasSequence = 0
        val instant = LocalDateTime.ofInstant(now, ZoneOffset.UTC)

        fun leaf(tagIds: List<String>, requireAll: Boolean): SqlFragment {
            val alias = "a${aliasSequence++}"
            val placeholders = tagIds.joinToString(", ") { "?" }
            val grouping = if (requireAll) {
                " group by $alias.subject_id having count(distinct $alias.tag_id) = ?"
            } else {
                ""
            }
            return SqlFragment(
                "exists (select 1 from tag_assignment $alias " +
                    "where $alias.tenant_id = ? and $alias.subject_type = ? " +
                    "and $alias.subject_id = s.subject_id and $alias.tag_id in ($placeholders) " +
                    "and ($alias.effective_from is null or $alias.effective_from <= ?) " +
                    "and ($alias.effective_until is null or $alias.effective_until > ?)$grouping)",
                buildList {
                    add(tenantId)
                    add(subjectType)
                    addAll(tagIds)
                    add(instant)
                    add(instant)
                    if (requireAll) add(tagIds.size)
                },
            )
        }

        fun resolved(codes: Set<String>): List<String> = codes.sorted().map { code ->
            requireNotNull(tagIdsByCode[code]) { "Tag [$code] was not resolved before query compilation." }
        }

        fun compileExpression(node: TagQueryExpression): SqlFragment = when (node) {
            is AllTags -> {
                val codes = resolved(node.tagCodes)
                val fragments = buildList {
                    if (codes.size >= groupedAllTagsThreshold) add(leaf(codes, requireAll = true))
                    else codes.forEach { add(leaf(listOf(it), requireAll = false)) }
                    node.nested.forEach { add(compileExpression(it)) }
                }
                fragments.join(" and ")
            }
            is AnyTags -> buildList {
                if (node.tagCodes.isNotEmpty()) add(leaf(resolved(node.tagCodes), requireAll = false))
                node.nested.forEach { add(compileExpression(it)) }
            }.join(" or ")
            is NotTags -> compileExpression(node.child).let { child ->
                SqlFragment("not (${child.sql})", child.parameters)
            }
        }

        val predicate = compileExpression(expression)
        val cursorSql = page.afterSubjectId?.let { " and s.subject_id > ?" }.orEmpty()
        val sql = "select s.subject_id from tag_subject s " +
            "where s.tenant_id = ? and s.subject_type = ?$cursorSql and (${predicate.sql}) " +
            "order by s.subject_id asc limit ?"
        return CompiledTagAssignmentQuery(
            sql,
            buildList {
                add(tenantId)
                add(subjectType)
                page.afterSubjectId?.let(::add)
                addAll(predicate.parameters)
                add(page.size + 1)
            },
        )
    }

    private data class SqlFragment(val sql: String, val parameters: List<Any>) {
        fun parenthesized() = "($sql)"
    }

    private fun List<SqlFragment>.join(operator: String): SqlFragment {
        require(isNotEmpty()) { "A validated query group cannot compile to an empty predicate." }
        return SqlFragment(
            joinToString(operator, transform = SqlFragment::parenthesized),
            flatMap(SqlFragment::parameters),
        )
    }
}

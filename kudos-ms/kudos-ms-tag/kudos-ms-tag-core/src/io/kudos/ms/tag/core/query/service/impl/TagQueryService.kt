package io.kudos.ms.tag.core.query.service.impl

import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.TagValidationException
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.query.model.TagQueryExpression.AllTags
import io.kudos.ms.tag.common.query.model.TagQueryExpression.AnyTags
import io.kudos.ms.tag.common.query.model.TagQueryExpression.NotTags
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.query.service.iservice.ITagQueryService
import io.kudos.ms.tag.core.runtime.port.TagAssignmentIndex
import io.kudos.ms.tag.core.runtime.port.TagKeysetPage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
open class TagQueryService(
    private val tagDao: TagDefinitionDao,
    private val assignmentIndex: TagAssignmentIndex,
) : ITagQueryService {

    override fun findSubjects(request: TagQueryRequest): TagSubjectPage {
        validateComplexity(request.expression)
        validateTags(request.tenantId, request.subjectType, request.expression)
        val result = assignmentIndex.search(
            request.tenantId,
            request.subjectType,
            request.expression,
            TagKeysetPage(request.afterSubjectId, request.pageSize),
        )
        return TagSubjectPage(result.subjectIds, result.nextSubjectId)
    }

    private fun validateComplexity(expression: TagQueryExpression) {
        if (expression.depth() > MAX_DEPTH) {
            fail(TagErrorCode.QUERY_LIMIT_EXCEEDED, "Tag query depth must not exceed $MAX_DEPTH.")
        }
        if (expression.codeReferenceCount() > MAX_TAG_CODES) {
            fail(TagErrorCode.QUERY_LIMIT_EXCEEDED, "Tag query must not contain more than $MAX_TAG_CODES tag codes.")
        }
    }

    private fun validateTags(tenantId: String, subjectType: String, expression: TagQueryExpression) {
        val codes = expression.tagCodes()
        val candidatesByCode = tagDao.listByTenantAndCodes(tenantId, codes).groupBy { it.code }
        codes.forEach { code ->
            val candidates = candidatesByCode[code].orEmpty()
            val tag = candidates.firstOrNull { it.subjectType == subjectType }
            when {
                tag == null && candidates.isNotEmpty() -> fail(
                    TagErrorCode.SUBJECT_TYPE_MISMATCH,
                    "Tag [$code] is not defined for subject type [$subjectType].",
                )
                tag == null || !tag.active -> fail(
                    TagErrorCode.QUERY_TAG_NOT_FOUND,
                    "Active tag [$code] does not exist for this tenant and subject type.",
                )
            }
        }
    }

    private fun fail(code: TagErrorCode, message: String): Nothing = throw TagValidationException(code, message)

    private fun TagQueryExpression.depth(): Int = when (this) {
        is AllTags -> 1 + (nested.maxOfOrNull { it.depth() } ?: 0)
        is AnyTags -> 1 + (nested.maxOfOrNull { it.depth() } ?: 0)
        is NotTags -> 1 + child.depth()
    }

    private fun TagQueryExpression.codeReferenceCount(): Int = when (this) {
        is AllTags -> tagCodes.size + nested.sumOf { it.codeReferenceCount() }
        is AnyTags -> tagCodes.size + nested.sumOf { it.codeReferenceCount() }
        is NotTags -> child.codeReferenceCount()
    }

    private fun TagQueryExpression.tagCodes(): Set<String> = when (this) {
        is AllTags -> tagCodes + nested.flatMapTo(linkedSetOf()) { it.tagCodes() }
        is AnyTags -> tagCodes + nested.flatMapTo(linkedSetOf()) { it.tagCodes() }
        is NotTags -> child.tagCodes()
    }

    private companion object {
        const val MAX_DEPTH = 10
        const val MAX_TAG_CODES = 100
    }
}

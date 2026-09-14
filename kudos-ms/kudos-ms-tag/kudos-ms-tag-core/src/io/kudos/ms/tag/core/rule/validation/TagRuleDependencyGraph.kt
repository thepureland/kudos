package io.kudos.ms.tag.core.rule.validation

import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.TagValidationException
import org.springframework.stereotype.Component

/** Pure directed-graph check used before a HasTag dependency is published. */
@Component
class TagRuleDependencyGraph {

    fun requireAcyclic(
        targetTagId: String,
        proposedDependencies: Set<String>,
        existingEdges: Map<String, Set<String>>,
    ) {
        val edges = existingEdges.toMutableMap().apply { put(targetTagId, proposedDependencies) }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun visit(tagId: String) {
            if (tagId in visiting) {
                throw TagValidationException(
                    TagErrorCode.RULE_DEPENDENCY_CYCLE,
                    "HasTag dependency cycle contains tag [$tagId].",
                )
            }
            if (!visited.add(tagId)) return
            visiting += tagId
            edges[tagId].orEmpty().forEach(::visit)
            visiting -= tagId
        }

        edges.keys.forEach(::visit)
    }
}

package io.kudos.ms.tag.core.runtime.job.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationCandidate
import io.kudos.ms.tag.core.runtime.job.model.table.TagRecalculationCandidates
import org.springframework.stereotype.Repository

@Repository
open class TagRecalculationCandidateDao :
    BaseCrudDao<String, TagRecalculationCandidate, TagRecalculationCandidates>()

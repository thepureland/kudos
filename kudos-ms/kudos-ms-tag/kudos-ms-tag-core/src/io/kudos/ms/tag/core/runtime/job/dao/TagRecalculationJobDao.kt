package io.kudos.ms.tag.core.runtime.job.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationJob
import io.kudos.ms.tag.core.runtime.job.model.table.TagRecalculationJobs
import org.ktorm.dsl.eq
import org.ktorm.entity.firstOrNull
import org.springframework.stereotype.Repository

@Repository
open class TagRecalculationJobDao : BaseCrudDao<String, TagRecalculationJob, TagRecalculationJobs>() {
    open fun findByJobKey(jobKey: String): TagRecalculationJob? =
        entitySequence().firstOrNull { TagRecalculationJobs.jobKey eq jobKey }
}

package io.kudos.ms.tag.core.runtime

import io.kudos.ms.tag.core.runtime.assignment.model.table.TagAssignmentEvents
import io.kudos.ms.tag.core.runtime.assignment.model.table.TagAssignments
import io.kudos.ms.tag.core.runtime.attribute.model.table.TagAttributeEvents
import io.kudos.ms.tag.core.runtime.attribute.model.table.TagAttributeStates
import io.kudos.ms.tag.core.runtime.job.model.table.TagRecalculationCandidates
import io.kudos.ms.tag.core.runtime.job.model.table.TagRecalculationJobs
import io.kudos.ms.tag.core.runtime.membership.model.table.TagMemberships
import io.kudos.ms.tag.core.runtime.subject.model.table.TagSubjects
import org.ktorm.schema.BaseTable
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RuntimePersistenceMappingTest {

    @Test
    fun runtimeTablesMapEverySchemaColumnAndNaturalPrimaryKey() {
        assertMapping(TagSubjects, "subject_id,tenant_id,subject_type,display_name,profile_json,state_version,first_seen_time,update_time", "subject_id,tenant_id,subject_type")
        assertMapping(TagAttributeEvents, "event_id,request_id,payload_checksum,tenant_id,subject_type,subject_id,attribute_id,operation,source_code,source_version,occurred_time,received_time,process_status,error_code,error_message,value_type,string_value,integer_value,decimal_value,boolean_value,date_value,datetime_value", "event_id")
        assertMapping(TagAttributeStates, "id,tenant_id,subject_type,subject_id,attribute_id,value_key,value_type,string_value,integer_value,decimal_value,boolean_value,date_value,datetime_value,source_event_id,source_version,state_version,effective_time,expire_time,update_time", "id")
        assertMapping(TagMemberships, "id,tenant_id,subject_type,subject_id,tag_id,source_type,source_ref,active,rule_version,effective_from,effective_until,membership_version,source_event_id,update_time", "id")
        assertMapping(TagAssignments, "tag_id,tenant_id,subject_type,subject_id,exclusive_set_id,assignment_version,evaluated_rule_version,materialized_time,effective_from,effective_until,update_time", "tag_id,tenant_id,subject_type,subject_id")
        assertMapping(TagAssignmentEvents, "event_id,tenant_id,subject_type,subject_id,tag_id,operation,cause_type,cause_ref,assignment_version,occurred_time", "event_id")
        assertMapping(TagRecalculationJobs, "id,job_key,tenant_id,job_type,tag_id,rule_version,subject_type,subject_id,cursor_subject_id,status,priority,requested_version,processed_version,attempt_count,max_attempts,available_time,lease_owner,lease_until,processed_count,last_error_code,last_error_message,create_time,start_time,complete_time,update_time,version", "id")
        assertMapping(TagRecalculationCandidates, "run_id,tenant_id,subject_type,subject_id,tag_id,rule_version,evaluated_time", "run_id,subject_type,subject_id,tag_id")
    }

    private fun assertMapping(table: BaseTable<*>, columns: String, primaryKeys: String) {
        assertEquals(columns.split(',').toSet(), table.columns.map { it.name }.toSet(), table.tableName)
        assertEquals(primaryKeys.split(',').toSet(), table.primaryKeys.map { it.name }.toSet(), "${table.tableName} primary key")
    }
}

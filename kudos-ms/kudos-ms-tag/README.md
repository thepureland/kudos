# Kudos Tag Service

`kudos-ms-tag` is a tenant-isolated, subject-neutral tag service. A subject is identified by
`(tenantId, subjectType, subjectId)`, so the same model supports houses, games, people, or any
other business object. Business predicates are defined and published as reusable rules; runtime
queries operate on materialized tags only.

The v1 runtime is deliberately RDB + JVM based. It does not require ClickHouse, Kafka, or Flink.

## Module selection

| Need | Depend on / run | Notes |
| --- | --- | --- |
| Shared contracts and models | `kudos-ms-tag-common` | No service implementation. |
| Embed tags in a business service | `kudos-ms-tag-core` | Local `ITag*Api` beans; no HTTP client or discovery dependency. |
| Call a standalone tag service | `kudos-ms-tag-client` | Spring HTTP service proxies and fallbacks. |
| Standalone internal API | `kudos-ms-tag-api-internal` | Fact ingestion, manual assignment, recalculation, query, and catalog reads. |
| Standalone management API | `kudos-ms-tag-api-admin` | Subject types, attributes, tag sets, tags, rules, and jobs. |
| Public edge | `kudos-ms-tag-api-public` | Boundary reserved for explicitly approved public operations; v1 exposes none. |

Embedded usage:

```kotlin
dependencies {
    implementation(project(":kudos-ms:kudos-ms-tag:kudos-ms-tag-core"))
}
```

Inject the shared interfaces such as `ITagAttributeFactApi` and `ITagQueryApi`. Because core
contains the implementation, calls remain in-process and the host service owns deployment,
transactions, datasource, security context, scheduling, and capacity.

Standalone usage:

```kotlin
dependencies {
    implementation(project(":kudos-ms:kudos-ms-tag:kudos-ms-tag-client"))
}
```

```yaml
spring:
  http:
    serviceclient:
      tag:
        base-url: http://kudos-ms-tag-api-internal:8080
```

Run `TagApiInternalApplication` for runtime traffic and `TagApiAdminApplication` for management
traffic. Both processes must point to the same tag datasource. Normal Kudos service discovery and
signed internal-call context may be used instead of a fixed base URL.

## Datasource and Flyway

Add the SQL module to the runtime classpath (it is already transitively included by core) and map
the Flyway module `tag` to the datasource key used by the host:

```yaml
spring:
  datasource:
    dynamic:
      primary: main
      datasource:
        main:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://mysql:3306/kudos_tag
          username: kudos_tag
          password: ${TAG_DB_PASSWORD}

kudos:
  ability:
    flyway:
      datasource-config:
        main: tag
```

Flyway scripts follow the Kudos convention `classpath:sql/tag/<database>/V*.sql`. The module ships
equivalent migrations for `h2`, `mysql`, and `postgresql`. A host must configure exactly one
matching datasource/dialect; schema creation is not performed ad hoc by the service.

## Fact ingress

Submit facts to `POST /api/internal/tag/facts`. `eventId` is the idempotency key. `subjectKey` is
always complete, `attributeCode` is a stable reusable definition, and `sourceVersion` can reject
out-of-order changes from versioned producers.

The following payload demonstrates every value type and every operation. `ADD`, `APPEND`, and
`REMOVE` require a multi-valued attribute; `CLEAR` is the only operation whose `value` must be
`null`.

```json
[
  {
    "eventId": "e-string-set",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "district",
    "operation": "SET",
    "value": {"type": "string", "value": "shinjuku"},
    "occurredAt": "2026-09-14T00:00:00Z",
    "sourceCode": "estate",
    "sourceVersion": 1
  },
  {
    "eventId": "e-integer-set",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "bedrooms",
    "operation": "SET",
    "value": {"type": "integer", "value": 3},
    "occurredAt": "2026-09-14T00:00:01Z",
    "sourceCode": "estate"
  },
  {
    "eventId": "e-decimal-set",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "area",
    "operation": "SET",
    "value": {"type": "decimal", "value": "108.50"},
    "occurredAt": "2026-09-14T00:00:02Z",
    "sourceCode": "estate"
  },
  {
    "eventId": "e-boolean-set",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "terrace",
    "operation": "SET",
    "value": {"type": "boolean", "value": true},
    "occurredAt": "2026-09-14T00:00:03Z",
    "sourceCode": "estate"
  },
  {
    "eventId": "e-date-set",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "available_date",
    "operation": "SET",
    "value": {"type": "date", "value": "2026-10-01"},
    "occurredAt": "2026-09-14T00:00:04Z",
    "sourceCode": "estate"
  },
  {
    "eventId": "e-datetime-set",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "last_viewed_at",
    "operation": "SET",
    "value": {"type": "datetime", "value": "2026-09-14T09:00:00+09:00"},
    "occurredAt": "2026-09-14T00:00:05Z",
    "sourceCode": "estate"
  },
  {
    "eventId": "e-add",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "features",
    "operation": "ADD",
    "value": {"type": "string", "value": "terrace"},
    "occurredAt": "2026-09-14T00:00:06Z",
    "sourceCode": "estate"
  },
  {
    "eventId": "e-append",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "view_history",
    "operation": "APPEND",
    "value": {"type": "string", "value": "agent-7"},
    "occurredAt": "2026-09-14T00:00:07Z",
    "sourceCode": "estate"
  },
  {
    "eventId": "e-remove",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "features",
    "operation": "REMOVE",
    "value": {"type": "string", "value": "terrace"},
    "occurredAt": "2026-09-14T00:00:08Z",
    "sourceCode": "estate"
  },
  {
    "eventId": "e-clear",
    "subjectKey": {"tenantId": "tenant-a", "subjectType": "estate.house", "subjectId": "h-1"},
    "attributeCode": "view_history",
    "operation": "CLEAR",
    "value": null,
    "occurredAt": "2026-09-14T00:00:09Z",
    "sourceCode": "estate"
  }
]
```

## Reusable rules and tag-only queries

Create the target tag first, then create and publish an immutable rule version. For example,
`POST /api/admin/tag/tag`:

```json
{
  "tenantId": "tenant-a",
  "subjectType": "estate.house",
  "code": "estate_candidate",
  "name": "3 rooms, 100㎡+, terrace or rooftop",
  "setPriority": 0,
  "manualAssignable": false
}
```

Create its draft with `POST /api/admin/tag/rule/draft`, then publish it with
`POST /api/admin/tag/rule/publish?tenantId=tenant-a&ruleId=<rule-id>`:

```json
{
  "tenantId": "tenant-a",
  "tagCode": "estate_candidate",
  "expression": {
    "type": "all",
    "children": [
      {
        "type": "attribute",
        "attributeCode": "bedrooms",
        "operator": "EQ",
        "operands": [{"type": "integer", "value": 3}]
      },
      {
        "type": "attribute",
        "attributeCode": "area",
        "operator": "GTE",
        "operands": [{"type": "decimal", "value": "100"}]
      },
      {
        "type": "any",
        "children": [
          {
            "type": "attribute",
            "attributeCode": "terrace",
            "operator": "EQ",
            "operands": [{"type": "boolean", "value": true}]
          },
          {
            "type": "attribute",
            "attributeCode": "rooftop",
            "operator": "EQ",
            "operands": [{"type": "boolean", "value": true}]
          }
        ]
      }
    ]
  }
}
```

The composite rule above is useful when callers always consume one named segment. To maximize
reuse, define four atomic rule tags (`three_bedrooms`, `area_100_plus`, `has_terrace`, and
`has_rooftop`) and combine those materialized tags at query time. After recalculation, query
`POST /api/internal/tag/query`. Query expressions accept only tag codes (`allTags`, `anyTags`, and
`notTags`), never runtime attribute predicates:

```json
{
  "tenantId": "tenant-a",
  "subjectType": "estate.house",
  "expression": {
    "type": "allTags",
    "tagCodes": ["three_bedrooms", "area_100_plus"],
    "nested": [
      {
        "type": "anyTags",
        "tagCodes": ["has_terrace", "has_rooftop"],
        "nested": []
      }
    ]
  },
  "afterSubjectId": null,
  "pageSize": 100
}
```

Game (`gomoku AND one_v_one AND online_play`) and HR (`age_30_40`, whose rule uses inclusive
`BETWEEN 30, 40`) follow the same pattern. Rules are reusable configuration; querying remains a
bounded keyset-paged lookup over materialized assignments.

## Recalculation and expiry scheduling

Workers are disabled by default so embedding does not silently create background load. Enable them
explicitly in exactly the deployment that owns runtime processing:

```yaml
kudos:
  tag:
    recalculation:
      scheduling-enabled: true
      batch-size: 20
      lease-duration: 1m
      poll-delay: 5s
      maximum-attempts: 10
      synchronous-subject-limit: 100
      synchronous-direct-rule-limit: 200
      expiry-scheduling-enabled: true
      expiry-batch-size: 100
      expiry-poll-delay: 1m
```

Multiple standalone instances may share the queue: jobs use bounded batches, leases, retries, and
idempotent version checks. Embedded hosts should normally enable workers in one designated service
or use the synchronous recalculation API for bounded requests.

## Pulling facts from a business source

Push-based fact ingress is the default. For systems that must be polled, register a Spring bean
implementing `AttributeSourceProvider`; core discovers providers by stable `providerCode`:

```kotlin
@Component
class HrAttributeSource(
    private val employees: EmployeeRepository,
) : AttributeSourceProvider {
    override val providerCode = "hr"

    override fun validate(config: AttributeSourceConfig): List<ValidationError> =
        if (config.subjectType == "hr.person") emptyList()
        else listOf(ValidationError("subject_type", "Expected hr.person"))

    override fun fetch(
        config: AttributeSourceConfig,
        cursor: SourceCursor?,
        limit: Int,
    ): AttributeSourceBatch {
        val page = employees.fetchAfter(cursor?.value, limit)
        return AttributeSourceBatch(
            facts = page.items.map { employee ->
                TagAttributeFact(
                    eventId = "hr-age-${employee.id}-${employee.version}",
                    subjectKey = TagSubjectKey(config.tenantId, config.subjectType, employee.id),
                    attributeCode = "age",
                    operation = TagAttributeOperation.SET,
                    value = TagAttributeValue.IntegerValue(employee.age.toLong()),
                    occurredAt = employee.updatedAt,
                    sourceCode = providerCode,
                    sourceVersion = employee.version,
                )
            },
            nextCursor = page.nextId?.let(::SourceCursor),
            exhausted = page.nextId == null,
        )
    }
}
```

The refresh service rejects provider output that escapes the configured tenant, subject type, or
provider code.

## Explicit v1 non-goals

- No ClickHouse query/materialization adapter.
- No Kafka fact transport.
- No Flink streaming evaluator.
- No user-supplied or arbitrary runtime attribute predicates in the query API.
- No public controllers until a public use case and security contract are approved.

These are extension points, not prerequisites. The stable core is the subject identity, typed fact
contract, versioned rule tree, materialized assignment model, and tag-only Boolean query algebra.

# Kudos Generic Tag Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a reusable, materialized tag service that can tag any tenant-scoped subject, supports versioned reusable segments and tag-only Boolean queries, and works both embedded in a business service and as an independently deployed atomic service.

**Architecture:** Follow the seven-module structure of `kudos-ms-sys`. Keep shared models and method-level HTTP contracts in `common`, Flyway migrations in `sql`, domain services and default RDB ports in `core`, remote proxies in `client`, and deployment boundaries in the three API modules. Attribute facts update typed state transactionally; durable recalculation produces source-isolated membership, then an assignment resolver materializes the final tag index.

**Tech Stack:** Kotlin 2.x, JVM 25, Spring Framework/Spring Boot, Spring HTTP Service Client, Ktorm, Flyway, H2, MySQL, PostgreSQL, JUnit 5, Testcontainers, Micrometer when present.

**Spec:** `docs/superpowers/specs/2026-09-14-kudos-tag-abstraction-design.md`

## Global Constraints

- Preserve the exact seven modules: `common`, `sql`, `core`, `client`, `api-public`, `api-admin`, and `api-internal`.
- Keep `kudos-ms-tag-core` independently embeddable. It must not depend on discovery, Nacos, the HTTP client module, or any API application module.
- Do not add ClickHouse, Kafka, Flink, Redis-only runtime state, arbitrary user-authored query predicates, or built-in `AttributeSourceProvider` implementations in v1.
- Every tenant-owned read and write must include `tenant_id`; never infer a tenant from `subject_id` or a cross-service foreign key.
- Use application-generated UUID strings for IDs and UTC `timestamp(6)` values. Do not depend on database UUID defaults.
- Rule versions are immutable after publication. A rule edit creates a new draft version.
- Remote fallback must throw a typed availability exception. It must never return `false`, an empty collection, `null`, or a fake successful write on transport failure.
- Tests use the repository's `src`, `test-src`, `resources`, and `test-resources` layout.
- Commit after every task only after its focused verification passes. Do not mix unrelated cleanup into these commits.

## File and Package Map

```text
kudos-ms/kudos-ms-tag/
├── kudos-ms-tag-common/
│   ├── src/io/kudos/ms/tag/common/
│   │   ├── subject/model/
│   │   ├── attribute/model/
│   │   ├── rule/model/
│   │   ├── assignment/model/
│   │   ├── query/model/
│   │   ├── catalog/model/
│   │   ├── error/
│   │   ├── fact/api/ITagAttributeFactApi.kt
│   │   ├── assignment/api/ITagAssignmentApi.kt
│   │   ├── query/api/ITagQueryApi.kt
│   │   └── catalog/api/ITagCatalogApi.kt
│   └── test-src/io/kudos/ms/tag/common/
├── kudos-ms-tag-sql/resources/sql/tag/{h2,mysql,postgresql}/
├── kudos-ms-tag-core/
│   ├── src/io/kudos/ms/tag/core/
│   │   ├── platform/init/TagAutoConfiguration.kt
│   │   ├── catalog/{model,dao,service,cache}/
│   │   ├── rule/{model,dao,service,validation,engine}/
│   │   ├── runtime/{model,dao,port,rdb,service,job,metrics}/
│   │   ├── fact/service/
│   │   ├── assignment/service/
│   │   ├── query/service/
│   │   └── api/
│   └── test-src/io/kudos/ms/tag/core/
├── kudos-ms-tag-client/src/io/kudos/ms/tag/client/{init,proxy,fallback}/
├── kudos-ms-tag-api-public/src/io/kudos/ms/tag/api/public/
├── kudos-ms-tag-api-admin/src/io/kudos/ms/tag/api/admin/
└── kudos-ms-tag-api-internal/src/io/kudos/ms/tag/api/internal/
```

---

### Task 1: Scaffold the seven-module service boundary

**Files:**

- Modify: `settings.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/build.gradle.kts`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-sql/build.gradle.kts`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/build.gradle.kts`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-client/build.gradle.kts`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-public/build.gradle.kts`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-admin/build.gradle.kts`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-internal/build.gradle.kts`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/platform/init/TagAutoConfiguration.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/platform/init/TagAutoConfigurationTest.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-public/src/io/kudos/ms/tag/api/public/init/TagApiPublicAutoConfiguration.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-admin/src/io/kudos/ms/tag/api/admin/init/TagApiAdminAutoConfiguration.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-internal/src/io/kudos/ms/tag/api/internal/init/TagApiInternalAutoConfiguration.kt`

- [ ] **Step 1: Prove the module does not exist yet**

Run:

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagAutoConfigurationTest'
```

Expected: FAIL with `project ... not found`.

- [ ] **Step 2: Register all seven projects and add minimal dependency graphs**

Use these dependency directions:

```text
common -> kudos-context
sql -> no project dependency
core -> sql + common + cache-common + data-rdb-ktorm + data-rdb-flyway
client -> common + distributed-client-http
api-public -> core + web-springmvc
api-admin -> core + web-springmvc
api-internal -> core + cache-interservice-provider + discovery-nacos + config-nacos + web-springmvc
```

Add `ktorm-support-mysql` beside the existing Ktorm catalog entries and `flyway-mysql` beside `flyway-database-postgresql`. Add Flyway's MySQL database module to tag core's runtime API; add H2, PostgreSQL, `com.mysql:mysql-connector-j`, Ktorm PostgreSQL/MySQL support, and `kudos-test-rdb`/`kudos-test-container` only to test configurations. Let the Spring Boot BOM manage the MySQL driver and Flyway module versions. Copy the JVM 25 and `-Xjvm-default=all` compiler configuration from `kudos-ms-sys-common` into tag common.

- [ ] **Step 3: Add the core initializer and its focused test**

```kotlin
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.tag.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class TagAutoConfiguration : IComponentInitializer {
    override fun getComponentName() = "kudos-ms-tag-core"
}
```

Test `getComponentName()` and assert each API initializer returns its own module name.

- [ ] **Step 4: Verify module discovery and compilation**

Run:

```powershell
./gradlew projects
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagAutoConfigurationTest'
```

Expected: all seven projects are listed and the focused test passes.

- [ ] **Step 5: Commit**

```powershell
git add settings.gradle.kts gradle/libs.versions.toml kudos-ms/kudos-ms-tag
git commit -m "build(tag): scaffold seven-module tag service"
```

---

### Task 2: Define shared typed values, facts, rule AST, and query expressions

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/subject/model/TagSubjectKey.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/attribute/model/TagAttributeType.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/attribute/model/TagAttributeCardinality.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/attribute/model/TagAttributeOperation.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/attribute/model/TagAttributeValue.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/attribute/model/TagAttributeFact.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/rule/model/TagRuleExpression.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/rule/model/TagRuleOperator.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/query/model/TagQueryExpression.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/assignment/model/TagMembershipSource.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/assignment/model/TagSetMode.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/error/TagErrorCode.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/test-src/io/kudos/ms/tag/common/model/TagModelContractTest.kt`

- [ ] **Step 1: Write the failing serialization and validation tests**

Cover:

```kotlin
val key = TagSubjectKey("tenant-a", "estate.house", "house-42")
val rule: TagRuleExpression = TagRuleExpression.AllOf(
    listOf(
        TagRuleExpression.AttributePredicate("bedrooms", EQ, listOf(IntegerValue(3))),
        TagRuleExpression.AttributePredicate("area", GTE, listOf(DecimalValue("100.0"))),
        TagRuleExpression.AnyOf(
            listOf(
                TagRuleExpression.AttributePredicate("terrace", EQ, listOf(BooleanValue(true))),
                TagRuleExpression.AttributePredicate("rooftop", EQ, listOf(BooleanValue(true))),
            )
        ),
    )
)
```

Assert round-trip serialization retains subtype discriminators and decimal precision. Assert blank tenant/type/id, invalid namespaced subject type, empty `AllOf`/`AnyOf`, and a `Between` operand with anything other than two values are rejected with stable `TagErrorCode` values.

- [ ] **Step 2: Run the focused test to confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-common:test --tests '*TagModelContractTest'
```

Expected: FAIL because the shared model types do not exist.

- [ ] **Step 3: Implement immutable shared models**

Use `@Serializable` and `@SerialName` on sealed hierarchies. Store decimal payloads as canonical strings and expose conversion to `BigDecimal`; use ISO-8601 strings for wire-level date/datetime values.

```kotlin
@Serializable
sealed interface TagRuleExpression {
    @Serializable @SerialName("all")
    data class AllOf(val children: List<TagRuleExpression>) : TagRuleExpression

    @Serializable @SerialName("any")
    data class AnyOf(val children: List<TagRuleExpression>) : TagRuleExpression

    @Serializable @SerialName("not")
    data class Not(val child: TagRuleExpression) : TagRuleExpression

    @Serializable @SerialName("attribute")
    data class AttributePredicate(
        val attributeCode: String,
        val operator: TagRuleOperator,
        val operands: List<TagAttributeValue>,
    ) : TagRuleExpression

    @Serializable @SerialName("hasTag")
    data class HasTag(val tagCode: String) : TagRuleExpression
}
```

Model query expressions independently as `AllTags`, `AnyTags`, and `NotTags`; do not reuse the attribute rule AST in query requests. Use the following exact recursive shape so `A AND B AND (C OR D)` needs no fourth node type:

```kotlin
@Serializable
sealed interface TagQueryExpression {
    @Serializable @SerialName("allTags")
    data class AllTags(
        val tagCodes: Set<String> = emptySet(),
        val nested: List<TagQueryExpression> = emptyList(),
    ) : TagQueryExpression

    @Serializable @SerialName("anyTags")
    data class AnyTags(
        val tagCodes: Set<String> = emptySet(),
        val nested: List<TagQueryExpression> = emptyList(),
    ) : TagQueryExpression

    @Serializable @SerialName("notTags")
    data class NotTags(val child: TagQueryExpression) : TagQueryExpression
}
```

Validation rejects an `AllTags`/`AnyTags` with both collections empty.

- [ ] **Step 4: Verify common contracts**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-common:test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-common
git commit -m "feat(tag): define shared tag domain contracts"
```

---

### Task 3: Add portable control-plane and runtime migrations

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-sql/resources/sql/tag/h2/V1.0.0.0__init_tag_control_plane.sql`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-sql/resources/sql/tag/h2/V1.0.0.1__init_tag_runtime.sql`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-sql/resources/sql/tag/mysql/V1.0.0.0__init_tag_control_plane.sql`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-sql/resources/sql/tag/mysql/V1.0.0.1__init_tag_runtime.sql`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-sql/resources/sql/tag/postgresql/V1.0.0.0__init_tag_control_plane.sql`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-sql/resources/sql/tag/postgresql/V1.0.0.1__init_tag_runtime.sql`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/schema/TagSchemaContractTest.kt`

- [ ] **Step 1: Write a failing schema contract test**

For each dialect, migrate a fresh database and query metadata for these exact tables:

```text
tag_subject_type, tag_attribute_definition, tag_set, tag_definition,
tag_rule, tag_rule_node, tag_rule_operand, tag_rule_dependency,
tag_taxonomy_node, tag_taxonomy_tag, tag_subject, tag_attribute_event,
tag_attribute_state, tag_membership, tag_assignment, tag_assignment_event,
tag_recalculation_candidate, tag_recalculation_job
```

Also assert these invariants by attempting invalid inserts:

- duplicate `tag_subject_type.code` and `(tenant_id, subject_type, code)` in `tag_attribute_definition` fail;
- duplicate fact `event_id` fails;
- one membership per `(tenant_id, subject_type, subject_id, tag_id, source_type, source_ref)`;
- one assignment per `(tenant_id, subject_type, subject_id, tag_id)`;
- rule node parent and rule dependency foreign keys reject dangling rows;
- `requested_version >= processed_version` and tag-set priority is non-negative.

- [ ] **Step 2: Run H2 contract to confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagSchemaContractTest'
```

Expected: FAIL because no tag migrations exist.

- [ ] **Step 3: Implement all three dialects**

Use the table/column definitions and indexes from spec sections 6.2 and 6.3. Keep logical constraints equivalent across dialects. Use `char(36)` IDs without database defaults, `timestamp(6)` in UTC, `decimal(38, 12)` for numeric state/operands, and separate typed state columns (`string_value`, `integer_value`, `decimal_value`, `boolean_value`, `date_value`, `datetime_value`).

Critical indexes:

```text
tag_attribute_state(tenant_id, subject_type, attribute_id, subject_id)
tag_membership(tenant_id, subject_type, tag_id, active, subject_id)
tag_assignment(tenant_id, subject_type, tag_id, subject_id)
tag_rule_dependency(tenant_id, dependency_type, attribute_id, rule_id)
tag_rule_dependency(tenant_id, dependency_type, referenced_tag_id, rule_id)
tag_recalculation_job(status, available_at, lease_until, id)
tag_recalculation_candidate(run_id, subject_type, subject_id, tag_id)
```

- [ ] **Step 4: Verify all database contracts**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagSchemaContractTest'
```

Expected: PASS on H2 and on Docker-backed MySQL/PostgreSQL when Docker is installed; container cases use the repository's Docker condition annotation and skip only when Docker is unavailable.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-sql kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/schema
git commit -m "feat(tag): add portable tag schema migrations"
```

---

### Task 4: Map catalog and rule persistence through Ktorm

**Files:**

- Create PO/table/DAO triples under `kudos-ms-tag-core/src/io/kudos/ms/tag/core/catalog/` for:
  - `subjecttype`: `TagSubjectType.kt`, `TagSubjectTypes.kt`, `TagSubjectTypeDao.kt`
  - `attribute`: `TagAttributeDefinition.kt`, `TagAttributeDefinitions.kt`, `TagAttributeDefinitionDao.kt`
  - `tagset`: `TagSet.kt`, `TagSets.kt`, `TagSetDao.kt`
  - `tag`: `TagDefinition.kt`, `TagDefinitions.kt`, `TagDefinitionDao.kt`
  - `taxonomy`: `TagTaxonomyNode.kt`, `TagTaxonomyNodes.kt`, `TagTaxonomyNodeDao.kt`, `TagTaxonomyTag.kt`, `TagTaxonomyTags.kt`, `TagTaxonomyTagDao.kt`
- Create PO/table/DAO triples under `kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/` for:
  - `TagRule.kt`, `TagRules.kt`, `TagRuleDao.kt`
  - `TagRuleNode.kt`, `TagRuleNodes.kt`, `TagRuleNodeDao.kt`
  - `TagRuleOperand.kt`, `TagRuleOperands.kt`, `TagRuleOperandDao.kt`
  - `TagRuleDependency.kt`, `TagRuleDependencies.kt`, `TagRuleDependencyDao.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/catalog/TagCatalogDaoTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/rule/TagRuleDaoTest.kt`

- [ ] **Step 1: Write failing DAO tests**

Insert two tenants with the same subject/tag codes and assert tenant-scoped DAO methods never cross tenant boundaries. Persist a rule tree, operands, and dependencies and reload them in deterministic `position` order. Assert `tag_definition.code` cannot be changed through the service-facing DAO update method.

- [ ] **Step 2: Run and observe the missing mappings**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagCatalogDaoTest' --tests '*TagRuleDaoTest'
```

Expected: FAIL at compile time because the mappings are absent.

- [ ] **Step 3: Implement Ktorm entities and mappings**

Every mutable PO extends `IManagedDbEntity<String, T>` and has a `DbEntityFactory` companion. Every table extends `ManagedTable<T>`; every DAO extends `BaseCrudDao<String, T, Table>()`. Add explicit tenant-scoped lookup methods rather than relying on callers to append criteria:

```kotlin
fun findByCode(tenantId: String, subjectTypeCode: String, code: String): TagDefinition?
fun listBySubjectType(tenantId: String, subjectTypeCode: String): List<TagDefinition>
fun loadTree(tenantId: String, ruleId: String, version: Long): PersistedRuleTree
```

- [ ] **Step 4: Verify mappings**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagCatalogDaoTest' --tests '*TagRuleDaoTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/catalog kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/catalog kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/rule
git commit -m "feat(tag): map catalog and rule persistence"
```

---

### Task 5: Implement catalog services and platform-preset copying

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/catalog/model/TagCatalogCommands.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/catalog/model/TagCatalogViews.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/catalog/service/iservice/ITagCatalogService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/catalog/service/impl/TagCatalogService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/catalog/service/impl/TagPlatformPresetService.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/catalog/TagCatalogServiceTest.kt`

- [ ] **Step 1: Write failing service tests**

Cover subject type registration, typed attribute definitions, tag set validation, immutable tag codes, SINGLE tag-set default uniqueness, subject-type consistency, and copying a platform preset into tenant-owned rows with new UUIDs.

- [ ] **Step 2: Run the focused test**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagCatalogServiceTest'
```

Expected: FAIL because services do not exist.

- [ ] **Step 3: Implement transactional catalog services**

Use `@Service`, `@Transactional`, `BaseCrudService` where ordinary CRUD fits, and explicit commands where invariants span tables. Enforce:

```text
subject type code: lowercase namespace segments separated by dots
attribute/tag/tag-set code: immutable after creation
SINGLE set: at most one default tag
MULTIPLE set: no default tag
tag and tag set: same tenant and subject type
platform preset: copied, never referenced live by tenant rows
```

- [ ] **Step 4: Verify catalog behavior**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagCatalogServiceTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/catalog kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/catalog
git commit -m "feat(tag): implement tag catalog services"
```

---

### Task 6: Validate, persist, and publish immutable rule versions

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/validation/TagRuleValidator.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/validation/TagRuleDependencyGraph.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/service/iservice/ITagRuleService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/service/impl/TagRuleService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/model/PersistedRuleTree.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/rule/TagRuleValidatorTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/rule/TagRuleServiceTest.kt`

- [ ] **Step 1: Write failing validator and lifecycle tests**

Test operator/type compatibility, one operand for scalar comparison, two ordered operands for `BETWEEN`, non-empty `IN`, no operands for `EXISTS`, attribute/tag references limited to the same tenant and subject type, duplicate sibling positions, maximum depth 20, maximum nodes 500, and `HasTag` cycles including indirect `A -> B -> C -> A`.

Test lifecycle `create draft v1 -> validate v1 -> begin rebuild v1 -> complete publication v1 -> create draft v2`; any attempt to mutate v1 after validation begins must fail.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagRuleValidatorTest' --tests '*TagRuleServiceTest'
```

- [ ] **Step 3: Implement validator, graph, and transactional lifecycle**

```kotlin
interface ITagRuleService {
    fun createDraft(tenantId: String, tagCode: String, expression: TagRuleExpression): TagRuleView
    fun replaceDraft(tenantId: String, ruleId: String, expression: TagRuleExpression): TagRuleView
    fun requestPublication(tenantId: String, ruleId: String): TagRulePublication
    fun getPublished(tenantId: String, tagCode: String): TagRuleView?
}
```

Flatten the AST into node and operand rows in one transaction. Rebuild `tag_rule_dependency` from referenced attributes and tags on every draft replacement. `requestPublication` performs validation, canonical checksum generation, dependency indexing, and the `DRAFT -> VALIDATING -> REBUILDING` transition, then creates a full-rebuild job. Only Task 14's successful atomic promotion changes `tag_definition.published_rule_id` and marks the new rule `PUBLISHED`; published node and operand rows are never updated.

- [ ] **Step 4: Verify lifecycle and graph rules**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagRuleValidatorTest' --tests '*TagRuleServiceTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/rule
git commit -m "feat(tag): add immutable rule publishing"
```

---

### Task 7: Map runtime records and define replaceable runtime ports

**Files:**

- Create PO/table/DAO triples under `kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/` for:
  - `subject`: `TagSubject.kt`, `TagSubjects.kt`, `TagSubjectDao.kt`
  - `attribute`: `TagAttributeEvent.kt`, `TagAttributeEvents.kt`, `TagAttributeEventDao.kt`, `TagAttributeState.kt`, `TagAttributeStates.kt`, `TagAttributeStateDao.kt`
  - `membership`: `TagMembership.kt`, `TagMemberships.kt`, `TagMembershipDao.kt`
  - `assignment`: `TagAssignment.kt`, `TagAssignments.kt`, `TagAssignmentDao.kt`, `TagAssignmentEvent.kt`, `TagAssignmentEvents.kt`, `TagAssignmentEventDao.kt`
  - `job`: `TagRecalculationCandidate.kt`, `TagRecalculationCandidates.kt`, `TagRecalculationCandidateDao.kt`, `TagRecalculationJob.kt`, `TagRecalculationJobs.kt`, `TagRecalculationJobDao.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/AttributeStateStore.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/TagMembershipStore.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/TagAssignmentIndex.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/TagRuleEvaluator.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/RecalculationQueue.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/AttributeSourceProvider.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/RuntimePortModels.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/AttributeSourceModels.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/RuntimePortAutoConfigurationTest.kt`

- [ ] **Step 1: Write a failing application-context test**

Assert default RDB beans are present with only core/data dependencies, and that user-provided beans for each port replace the defaults via `@ConditionalOnMissingBean`.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*RuntimePortAutoConfigurationTest'
```

- [ ] **Step 3: Implement exact port boundaries**

```kotlin
interface AttributeStateStore {
    fun apply(fact: TagAttributeFact, definition: TagAttributeDefinitionView): AttributeApplyResult
    fun load(key: TagSubjectKey, attributeCodes: Set<String>): Map<String, List<TagAttributeValue>>
}

interface TagMembershipStore {
    fun replace(key: TagSubjectKey, tagId: String, source: TagMembershipSource, sourceRef: String, active: Boolean, version: Long)
    fun listActive(key: TagSubjectKey): List<TagMembershipView>
}

interface TagAssignmentIndex {
    fun replaceForSet(key: TagSubjectKey, tagSetId: String?, winners: List<ResolvedAssignment>): AssignmentDelta
    fun search(tenantId: String, subjectType: String, expression: TagQueryExpression, page: TagKeysetPage): AssignmentSearchResult
}

interface TagRuleEvaluator {
    fun evaluate(expression: TagRuleExpression, context: TagEvaluationContext): Boolean
}

interface RecalculationQueue {
    fun request(command: RecalculationRequest): String
    fun lease(workerId: String, limit: Int, leaseUntil: Instant): List<LeasedRecalculationJob>
}

interface AttributeSourceProvider {
    val providerCode: String
    fun validate(config: AttributeSourceConfig): List<ValidationError>
    fun fetch(config: AttributeSourceConfig, cursor: SourceCursor?, limit: Int): AttributeSourceBatch
}
```

`RuntimePortModels.kt` owns the internal records referenced by these ports: `AttributeApplyResult`, `TagEvaluationContext`, `TagMembershipView`, `ResolvedAssignment`, `AssignmentDelta`, `TagKeysetPage`, `AssignmentSearchResult`, `RecalculationRequest`, and `LeasedRecalculationJob`. `AttributeSourceModels.kt` owns `AttributeSourceConfig`, opaque `SourceCursor`, `AttributeSourceBatch`, and `ValidationError`. This keeps every task compilable before the public query DTOs are added in Task 11.

Register default RDB adapters in `TagAutoConfiguration`; register no `AttributeSourceProvider` default.

- [ ] **Step 4: Verify default and override wiring**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*RuntimePortAutoConfigurationTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime
git commit -m "feat(tag): add runtime persistence ports"
```

---

### Task 8: Ingest attribute facts idempotently and materialize typed state

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/RdbAttributeStateStore.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/fact/service/iservice/ITagAttributeFactService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/fact/service/impl/TagAttributeFactService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/fact/model/AttributeFactValidationException.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/fact/TagAttributeFactServiceTest.kt`

- [ ] **Step 1: Write failing transaction and idempotency tests**

Cover all legal operations:

```text
SINGLE STRING/BOOLEAN/DATE/DATETIME: SET, CLEAR
SINGLE INTEGER/DECIMAL: SET, ADD, CLEAR
MULTIPLE: SET, APPEND, REMOVE, CLEAR
```

Assert the same `eventId` plus the same payload checksum returns `DUPLICATE` and creates neither a second event nor a second job version; the same `eventId` plus a different checksum returns `IDEMPOTENCY_CONFLICT`. Assert unknown subject type/attribute, value type mismatch, illegal operation/cardinality combinations, older `sourceVersion`, and blank source code fail without partial state. Assert a successful fact inserts the subject, event, state, and recalculation request in one transaction. Assert a batch is non-atomic across items, runs pure validation before bounded transactional slices, and marks a whole slice retryable on transient database failure.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagAttributeFactServiceTest'
```

- [ ] **Step 3: Implement the fact transaction**

In this order inside one `@Transactional` method:

```text
validate tenant/subject/definition/value/operation
insert tag_attribute_event; on duplicate event_id compare checksum and return DUPLICATE or IDEMPOTENCY_CONFLICT
upsert tag_subject
lock or compare-and-swap the relevant tag_attribute_state rows
apply SET/ADD/APPEND/REMOVE/CLEAR
resolve dependent published rules through tag_rule_dependency
request or bump the stable recalculation job requested_version
return APPLIED with the resulting state version
```

For MULTIPLE values, one row represents one canonical typed value and `value_key` is its SHA-256; therefore `APPEND` has set-add semantics and cannot create duplicate occurrences. `ADD` is numeric increment protected from replay by `eventId`. `sourceVersion` guards non-commutative SET/CLEAR ordering; facts without it use service receive order and expose the documented weak-order guarantee.

- [ ] **Step 4: Verify ingestion behavior**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagAttributeFactServiceTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/fact kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/RdbAttributeStateStore.kt kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/fact
git commit -m "feat(tag): ingest typed attribute facts"
```

---

### Task 9: Evaluate rules with exact typed semantics

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/engine/JvmTagRuleEvaluator.kt`
- Modify: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/port/RuntimePortModels.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/engine/TagValueComparator.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/rule/engine/JvmTagRuleEvaluatorTest.kt`

- [ ] **Step 1: Write failing truth-table tests**

Cover `AllOf`, `AnyOf`, `Not`, all operators, missing values, SINGLE and MULTIPLE cardinality, decimal scale equality, UTC datetime ordering, string `CONTAINS`, and `HasTag`. Reproduce all three accepted scenarios: house segment, game segment, and age 30–40 segment.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*JvmTagRuleEvaluatorTest'
```

- [ ] **Step 3: Implement deterministic evaluation**

Rules:

```text
EXISTS / NOT_EXISTS inspect presence, not truthiness.
EQ on MULTIPLE is any-match; NE is no-match.
GT/GTE/LT/LTE/BETWEEN are valid only for ordered compatible types.
IN is any state value in operands; NOT_IN is no state value in operands.
CONTAINS means substring for STRING and membership for MULTIPLE values.
Missing values make positive comparisons false; NOT_EXISTS true.
HasTag reads active materialized assignment from the evaluation snapshot.
```

- [ ] **Step 4: Verify evaluator**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*JvmTagRuleEvaluatorTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/rule/engine kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/rule/engine
git commit -m "feat(tag): evaluate typed tag rules"
```

---

### Task 10: Resolve source-isolated memberships into final assignments

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/RdbTagMembershipStore.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/RdbTagAssignmentIndex.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/assignment/model/{po,table}/TagManualAssignmentEvent*.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/assignment/dao/TagManualAssignmentEventDao.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/assignment/service/iservice/ITagAssignmentService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/assignment/service/impl/TagAssignmentService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/assignment/service/impl/TagAssignmentResolver.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/assignment/TagAssignmentResolverTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/assignment/TagAssignmentServiceTest.kt`

- [ ] **Step 1: Write failing resolver tests**

Assert membership from RULE, MANUAL, IMPORT, and DEFAULT remains in four separate rows distinguished by `source_ref`. Removing one source never removes another. For a SINGLE set, assert priority `MANUAL > RULE > IMPORT > DEFAULT`, then tag `setPriority` descending, membership version descending, and tag ID ascending. For MULTIPLE, all active tags survive. Assert default activates only when no non-default winner exists and emits a corresponding assignment event. Manual assignment must reject a tag with `manual_assignable = false` and record operator, reason, request ID, event ID, and optional expiry without logging attribute values.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagAssignmentResolverTest' --tests '*TagAssignmentServiceTest'
```

- [ ] **Step 3: Implement resolver and atomic index replacement**

Use the `ResolvedAssignment` record defined with the runtime ports; it contains `tagId`, nullable `tagSetId`, `winningSource`, and `membershipVersion`. Expose explicit `assignManual(command)` and `removeManual(command)` service methods; use the caller-supplied event ID for idempotency and the manual event ID as `source_ref`. Lock the subject's assignments for the affected set, compute winners from active memberships, diff old/new assignments, write `tag_assignment_event` for ADD/REMOVE, and replace the index in the same transaction.

- [ ] **Step 4: Verify source precedence and events**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagAssignmentResolverTest' --tests '*TagAssignmentServiceTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/assignment kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/RdbTagMembershipStore.kt kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/RdbTagAssignmentIndex.kt kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/assignment
git commit -m "feat(tag): resolve final tag assignments"
```

---

### Task 11: Query the materialized assignment index with Boolean tag expressions

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/query/model/TagQueryRequest.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/query/model/TagSubjectPage.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/query/service/iservice/ITagQueryService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/query/service/impl/TagQueryService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/TagAssignmentQueryCompiler.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/query/TagQueryServiceTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/query/TagQueryDialectContractTest.kt`

- [ ] **Step 1: Write failing query tests**

Seed assignments and assert:

```kotlin
AllTags(
    tagCodes = setOf("three-bedroom", "area-100-plus"),
    nested = listOf(AnyTags(tagCodes = setOf("terrace", "rooftop"))),
)
AllTags(setOf("gomoku", "one-v-one", "online"))
AllTags(setOf("age-30-to-40"))
NotTags(AnyTags(setOf("suspended")))
```

Return only subject IDs for the requested tenant and subject type. Test stable keyset pagination by `subject_id` with no duplicates or skips after an unrelated concurrent insert. Reject unknown tags, cross-subject-type tags, depth over 10, more than 100 tag codes, and page size over 500.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagQueryServiceTest' --tests '*TagQueryDialectContractTest'
```

- [ ] **Step 3: Implement tag-only query compilation**

Compile leaves to `EXISTS`/`NOT EXISTS` on `tag_assignment`, with tenant and subject type predicates repeated in every subquery. Every leaf must filter `effective_until IS NULL OR effective_until > now`; correctness may not depend on the expiry worker. Use `GROUP BY ... HAVING count(distinct tag_id)` for large `AllTags` leaves when it is cheaper than multiple EXISTS clauses. Never join `tag_attribute_state` from this API.

- [ ] **Step 4: Verify all dialects**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagQueryServiceTest' --tests '*TagQueryDialectContractTest'
```

Expected: PASS on H2 and Docker-backed MySQL/PostgreSQL where available.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/query kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/query kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/TagAssignmentQueryCompiler.kt kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/query
git commit -m "feat(tag): query materialized tag assignments"
```

---

### Task 12: Implement durable recalculation leasing and retry semantics

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/RdbRecalculationQueue.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/RecalculationLeaseDialect.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/H2RecalculationLeaseDialect.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/MySqlRecalculationLeaseDialect.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb/PostgreSqlRecalculationLeaseDialect.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job/RecalculationRetryPolicy.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/job/RecalculationQueueContractTest.kt`

- [ ] **Step 1: Write failing queue contract tests**

Verify a stable job key reuses a live job and increments `requested_version`; a worker records `processed_version`; an update arriving during processing leaves the job runnable; two workers cannot lease the same job; expired leases are reclaimable; an old worker with an expired lease cannot commit after another worker re-leases the row; retry delay grows with a cap; attempts beyond the configured maximum become `FAILED`; cancellation does not allow a stale worker to publish results.

- [ ] **Step 2: Confirm failure on H2**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*RecalculationQueueContractTest'
```

- [ ] **Step 3: Implement dialect-specific leasing**

PostgreSQL and MySQL use `SELECT ... FOR UPDATE SKIP LOCKED`. H2 uses a transactionally guarded compare-and-swap update over ordered candidate IDs. All dialects transition only:

```text
PENDING -> RUNNING -> SUCCEEDED
PENDING -> RUNNING -> RETRY_WAIT -> RUNNING
PENDING/RUNNING/RETRY_WAIT -> FAILED
PENDING/RUNNING/RETRY_WAIT -> CANCELLED
```

On success, mark `SUCCEEDED` only when `processed_version >= requested_version`; otherwise return to `PENDING` without creating a second logical job.

- [ ] **Step 4: Verify queue contracts on all dialects**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*RecalculationQueueContractTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/rdb kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/job
git commit -m "feat(tag): add durable recalculation queue"
```

---

### Task 13: Execute incremental recalculation and bounded synchronous recalculation

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/service/iservice/ITagRecalculationService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/service/impl/TagRecalculationService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job/TagRecalculationWorker.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job/TagRecalculationScheduler.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/TagIncrementalRecalculationTest.kt`

- [ ] **Step 1: Write failing cascade and bound tests**

Assert one changed attribute recalculates only directly dependent published rules, then cascades through `HasTag` dependencies in topological order. Assert unchanged results produce no assignment event. Assert synchronous requests reject more than 100 subjects or more than 200 direct rules, and reject full rebuild requests.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagIncrementalRecalculationTest'
```

- [ ] **Step 3: Implement orchestration**

```kotlin
interface ITagRecalculationService {
    fun requestIncremental(keys: Collection<TagSubjectKey>, reason: String): List<String>
    fun recalculateSubjectNow(keys: Collection<TagSubjectKey>, directRuleLimit: Int = 200): RecalculationSummary
    fun process(job: LeasedRecalculationJob): RecalculationSummary
}
```

The scheduler must remain passive unless the embedding application enables scheduling. Expose batch size, lease duration, poll delay, maximum attempts, and synchronous limits under `kudos.tag.recalculation.*` with safe defaults.

- [ ] **Step 4: Verify incremental processing**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagIncrementalRecalculationTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/service kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/TagIncrementalRecalculationTest.kt
git commit -m "feat(tag): process incremental recalculation"
```

---

### Task 14: Build full-recalculation candidates and promote atomically

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job/FullRecalculationService.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job/TagCandidateBuilder.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job/TagCandidatePromoter.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/service/impl/AttributeSourceRefreshService.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/FullRecalculationServiceTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/AttributeSourceRefreshServiceTest.kt`

- [ ] **Step 1: Write failing publication/rebuild tests**

Request publication of v2 while v1 assignments are live. Assert candidate calculation can resume by `cursor_subject_id` without altering v1, persists only matching subjects, and promotes all membership/assignment changes in one transaction only after the full subject scan completes. A failed or cancelled job must leave v1 and `tag_definition.published_rule_id` unchanged. Assert an injected `AttributeSourceProvider` can feed facts through the same ingestion service, provider codes are unique, cursor values remain opaque to core, and no provider bean is required at startup.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*FullRecalculationServiceTest' --tests '*AttributeSourceRefreshServiceTest'
```

- [ ] **Step 3: Implement candidate build and promotion**

Use the exact candidate key `(run_id, subject_type, subject_id, tag_id)` and store only matches with `rule_version` and `evaluated_time`; an absent row means no match. Progress and retry state live on `tag_recalculation_job.cursor_subject_id` and `processed_count`. Promotion must verify the job still targets the REBUILDING version, replace that tag's RULE memberships using `source_ref = ruleId`, run assignment resolution, switch `tag_definition.published_rule_id`, mark the new rule `PUBLISHED`, retire the previous rule, write audit events, and complete the job atomically.

- [ ] **Step 4: Verify rebuild safety**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*FullRecalculationServiceTest' --tests '*AttributeSourceRefreshServiceTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime
git commit -m "feat(tag): promote full recalculations atomically"
```

---

### Task 15: Expose one local/remote API contract and fail closed remotely

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/fact/api/ITagAttributeFactApi.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/assignment/api/ITagAssignmentApi.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/query/api/ITagQueryApi.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/catalog/api/ITagCatalogApi.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-common/src/io/kudos/ms/tag/common/error/TagServiceUnavailableException.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/api/TagAttributeFactApi.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/api/TagAssignmentApi.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/api/TagQueryApi.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/api/TagCatalogApi.kt`
- Create proxy interfaces in `kudos-ms-tag-client/src/io/kudos/ms/tag/client/proxy/`: `ITagAttributeFactProxy.kt`, `ITagAssignmentProxy.kt`, `ITagQueryProxy.kt`, `ITagCatalogProxy.kt`
- Create fallbacks in `kudos-ms-tag-client/src/io/kudos/ms/tag/client/fallback/`: `TagAttributeFactFallback.kt`, `TagAssignmentFallback.kt`, `TagQueryFallback.kt`, `TagCatalogFallback.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-client/src/io/kudos/ms/tag/client/init/TagClientAutoConfiguration.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-client/test-src/io/kudos/ms/tag/client/TagClientContractTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-client/test-src/io/kudos/ms/tag/client/TagFallbackTest.kt`

- [ ] **Step 1: Write failing shared-contract round-trip and fallback tests**

Required method-level paths:

```text
POST /api/internal/tag/facts
POST /api/internal/tag/assignments/manual
DELETE /api/internal/tag/assignments/manual
POST /api/internal/tag/query
POST /api/internal/tag/recalculate/subjects
GET  /api/internal/tag/catalog/tags
```

As in `SysClientContractTest`, run a real servlet container whose mock controllers implement only the common interfaces. Assert serialization of sealed values/expressions and request parameters. Call every fallback and assert `TagServiceUnavailableException` with operation name and cause.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-client:test --tests '*TagClientContractTest' --tests '*TagFallbackTest'
```

- [ ] **Step 3: Implement shared APIs, local primary beans, proxies, and fail-closed fallbacks**

Put only method-level `@GetExchange`, `@PostExchange`, or `@DeleteExchange` annotations on common interfaces. Do not put `@RequestMapping` or `@HttpExchange` on interface types. Mark core implementations `@Primary @Component`. Configure the client with:

```kotlin
@ImportHttpServices(
    group = TagClientAutoConfiguration.GROUP,
    types = [
        ITagAttributeFactProxy::class,
        ITagAssignmentProxy::class,
        ITagQueryProxy::class,
        ITagCatalogProxy::class,
    ],
)
open class TagClientAutoConfiguration : IComponentInitializer {
    override fun getComponentName() = "kudos-ms-tag-client"
    companion object { const val GROUP = "tag" }
}
```

- [ ] **Step 4: Verify transport parity and fallback semantics**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-common:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-client:test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-common kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/api kudos-ms/kudos-ms-tag/kudos-ms-tag-client
git commit -m "feat(tag): expose local and remote tag APIs"
```

---

### Task 16: Add internal, admin, and public deployment boundaries

**Files:**

- Create internal controllers in `kudos-ms-tag-api-internal/src/io/kudos/ms/tag/api/internal/controller/`: `TagAttributeFactInternalController.kt`, `TagAssignmentInternalController.kt`, `TagQueryInternalController.kt`, `TagCatalogInternalController.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-internal/src/io/kudos/ms/tag/api/internal/init/TagApiInternalApplication.kt`
- Create admin controllers in `kudos-ms-tag-api-admin/src/io/kudos/ms/tag/api/admin/controller/`: `TagSubjectTypeAdminController.kt`, `TagAttributeAdminController.kt`, `TagSetAdminController.kt`, `TagDefinitionAdminController.kt`, `TagRuleAdminController.kt`, `TagRecalculationJobAdminController.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-admin/src/io/kudos/ms/tag/api/admin/init/TagApiAdminApplication.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-public/src/io/kudos/ms/tag/api/public/init/TagApiPublicApplication.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-admin/resources/application.yml`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-internal/resources/application.yml`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/security/TagTenantAccessGuard.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/security/TagSubjectWriteGuard.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-internal/test-src/io/kudos/ms/tag/api/internal/TagInternalApiTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-admin/test-src/io/kudos/ms/tag/api/admin/TagAdminApiTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-public/test-src/io/kudos/ms/tag/api/public/TagPublicBoundaryTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/security/TagAccessGuardTest.kt`

- [ ] **Step 1: Write failing web-slice tests**

Assert internal controllers inherit exactly the shared method-level mappings and delegate without altering results. Assert request tenant equals the authenticated tenant, ordinary callers cannot use platform tenant scope, and a service may submit facts only for a subject type whose `owner_service_code` matches its identity. Assert admin endpoints support catalog CRUD, rule draft/publish, job list/retry/cancel, enforce the permission codes below, and do not expose arbitrary attribute predicates as query inputs. Assert the public application starts with zero tag business endpoints in v1.

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-internal:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-admin:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-public:test
```

- [ ] **Step 3: Implement controllers and applications**

Use admin roots:

```text
/api/admin/tag/subject-type
/api/admin/tag/attribute
/api/admin/tag/tag-set
/api/admin/tag/tag
/api/admin/tag/rule
/api/admin/tag/recalculation-job
```

Internal controllers implement the common API interfaces and contain no mapping annotations of their own. Keep `api-public` as a deployable shell with initializer/application only.

Invoke `TagTenantAccessGuard` and `TagSubjectWriteGuard` from the core API beans as well as HTTP boundaries so embedded callers receive the same isolation rules. Reserve cross-tenant scope for an explicit platform-management authority. Enforce these permissions at admin entry points:

```text
tag:subject-type:view
tag:attribute:view, tag:attribute:manage
tag:definition:view, tag:definition:manage
tag:rule:view, tag:rule:manage, tag:rule:publish
tag:assignment:view, tag:assignment:manual
tag:job:view, tag:job:retry, tag:job:cancel
```

Built-in catalog rows cannot be deleted. Logs use `subjectIdHash`; never log full fact values, profile JSON, secrets, or bulk payloads.

- [ ] **Step 4: Verify deployment boundaries**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-internal:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-admin:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-public:test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-api-internal kudos-ms/kudos-ms-tag/kudos-ms-tag-api-admin kudos-ms/kudos-ms-tag/kudos-ms-tag-api-public kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/security kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/security
git commit -m "feat(tag): add service deployment boundaries"
```

---

### Task 17: Add versioned configuration cache, observability, and expiry maintenance

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/catalog/cache/TagCatalogCache.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/catalog/cache/PublishedRuleCache.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/metrics/TagRuntimeMetrics.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job/ExpiredMembershipWorker.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/runtime/job/TagRuntimeHealthIndicator.kt`
- Modify: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/platform/init/TagAutoConfiguration.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/catalog/TagCatalogCacheTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/TagRuntimeMetricsTest.kt`
- Test: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/runtime/ExpiredMembershipWorkerTest.kt`

- [ ] **Step 1: Write failing cache, metric, health, and expiry tests**

Assert cache keys include tenant, stable ID/code, and configuration/rule version. Assert publication changes only the short-lived current-rule pointer while immutable compiled versions remain addressable. Assert metrics have low-cardinality tags only and expose the exact counters/gauges/timers below. Assert health includes RDB, worker lease activity, oldest pending job, failed-job count, published-rule compilability, and Flyway version. Assert expired membership is deactivated, assignments are re-resolved, and events are emitted exactly once.

```text
tag_fact_received_total
tag_fact_duplicate_total
tag_fact_rejected_total
tag_job_pending
tag_job_processing_seconds
tag_job_retry_total
tag_job_failed_total
tag_rule_evaluation_seconds
tag_rule_match_total
tag_assignment_change_total
tag_materialization_lag_seconds
tag_query_seconds
tag_query_result_size
```

- [ ] **Step 2: Confirm failure**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagCatalogCacheTest' --tests '*TagRuntimeMetricsTest' --tests '*ExpiredMembershipWorkerTest'
```

- [ ] **Step 3: Implement optional integrations**

Cache only tag definitions, immutable rule versions/compiled rules, dependencies, tag sets, taxonomy, and a short-TTL current-rule pointer. Do not cache subject facts, attribute-state scans, membership, assignment pages, job lists, or query results. A cache miss reloads the exact requested rule version and never substitutes the newest version. Create Micrometer and health beans only when their classes/registries are present. Keep scheduled workers passive unless the host enables scheduling.

Never put tenant ID, subject ID, tag code, or rule ID into metric labels. Log them as structured fields with correlation/job/event IDs instead.

- [ ] **Step 4: Verify operational behavior**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*TagCatalogCacheTest' --tests '*TagRuntimeMetricsTest' --tests '*ExpiredMembershipWorkerTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add kudos-ms/kudos-ms-tag/kudos-ms-tag-core
git commit -m "feat(tag): add tag runtime operations support"
```

---

### Task 18: Prove embedded and standalone scenarios end to end

**Files:**

- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/scenario/EstateTagScenarioTest.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/scenario/GameTagScenarioTest.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/scenario/HrTagScenarioTest.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-core/test-src/io/kudos/ms/tag/core/scenario/EmbeddedTagServiceTest.kt`
- Create: `kudos-ms/kudos-ms-tag/kudos-ms-tag-api-internal/test-src/io/kudos/ms/tag/api/internal/StandaloneTagServiceTest.kt`
- Create: `kudos-ms/kudos-ms-tag/README.md`

- [ ] **Step 1: Write failing acceptance scenarios**

For each scenario, configure reusable tags/rules, push facts, process recalculation, and query only tags:

```text
estate.house: bedrooms = 3 AND area >= 100 AND (terrace = true OR rooftop = true)
game.catalog: gameplay = gomoku AND mode = 1v1 AND online = true
hr.person: age BETWEEN 30 AND 40
```

Assert tenant isolation by using identical subject IDs in two tenants. Assert local API calls work with `core` embedded and no client/discovery dependency. Assert the standalone internal app plus `client` produces the same results over HTTP.

- [ ] **Step 2: Confirm at least one scenario fails before its fixture is complete**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test --tests '*ScenarioTest' --tests '*EmbeddedTagServiceTest'
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-internal:test --tests '*StandaloneTagServiceTest'
```

- [ ] **Step 3: Complete fixtures and document usage**

The README must include:

```text
module selection for embedded and standalone deployment
required datasource/Flyway locations
fact JSON examples for every value type and operation
rule and tag-query JSON examples
client base-url configuration under spring.http.serviceclient.tag
scheduler enablement and kudos.tag.recalculation properties
AttributeSourceProvider extension example
explicit v1 non-goals: ClickHouse, Kafka, Flink, arbitrary runtime predicates
```

- [ ] **Step 4: Run focused and full tag verification**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-common:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-client:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-public:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-admin:test :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-internal:test
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-common:build :kudos-ms:kudos-ms-tag:kudos-ms-tag-sql:build :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:build :kudos-ms:kudos-ms-tag:kudos-ms-tag-client:build :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-public:build :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-admin:build :kudos-ms:kudos-ms-tag:kudos-ms-tag-api-internal:build
```

Expected: PASS. Docker-backed tests may be skipped only through the repository's Docker availability condition.

- [ ] **Step 5: Check architecture boundaries and forbidden scope**

```powershell
./gradlew :kudos-ms:kudos-ms-tag:kudos-ms-tag-core:dependencies
rg -n "clickhouse|kafka|flink" kudos-ms/kudos-ms-tag
rg -n "tag_attribute_state" kudos-ms/kudos-ms-tag/kudos-ms-tag-core/src/io/kudos/ms/tag/core/query
```

Expected: `core` has no client/discovery/API dependencies; the forbidden technology scan has no functional dependency or implementation hits; the query package does not access attribute state.

- [ ] **Step 6: Commit**

```powershell
git add kudos-ms/kudos-ms-tag
git commit -m "test(tag): verify embedded and standalone scenarios"
```

---

## Final Review Gate

- [ ] Map every acceptance criterion in spec section 15 to at least one named test above.
- [ ] Confirm source precedence and SINGLE-set tie-breaking have explicit deterministic tests.
- [ ] Confirm every API read/write and every DAO lookup has a tenant-isolation test.
- [ ] Confirm the public module exposes no v1 business endpoints.
- [ ] Confirm there is no permanent-deduplication path that can suppress a new `requested_version`.
- [ ] Confirm no published rule row, node, or operand is updated in place.
- [ ] Confirm remote fallback never turns unavailable into empty/false/null/success.
- [ ] Run the placeholder scan:

```powershell
rg -n "TODO|TBD|FIXME|placeholder|implement later|similar to" kudos-ms/kudos-ms-tag
```

Expected: no unresolved implementation placeholders.

- [ ] Run whitespace and repository checks:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; only intentional tag-service changes remain before the final commit.

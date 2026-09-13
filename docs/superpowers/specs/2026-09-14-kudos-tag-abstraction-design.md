# Kudos 通用標籤抽象層設計

> 狀態：初稿，待書面評審
>
> 日期：2026-09-14
>
> 適用範圍：新建 `kudos-ms-tag` 原子服務及其嵌入式使用方式
>
> 首版技術基線：Kotlin/JVM + Spring + Ktorm + RDB（H2/MySQL/PostgreSQL）

## 1. 背景與結論

Kudos 需要一套不綁定玩家或任何單一業務域的標籤能力，支援房產、遊戲、人事等任意對象，主要服務於預先定義、可重用並可物化的分群。設計借鑑 Soul 的通用 `scope + objId`、屬性標籤、組合標籤和可替換計算/存儲方向，同時修正其對象鍵不完整、類型維度混合、規則關係隱式、事件不可靠、輕量實現空殼和資料表約束不足等問題。

本設計選擇：

- 外部模組嚴格參照 `kudos-ms-sys` 的七模組結構；
- 同一 common API 同時支援 core 本地嵌入和 client 遠程調用；
- 首版完整交付 JVM 規則引擎與 RDB 運行面；
- 標籤命中結果預先物化，普通查詢不臨時計算屬性規則；
- 業務服務主動推送標準化 Attribute Fact，拉取式來源只保留 SPI；
- Kafka、ClickHouse、Flink 不在首版落地，也不建立空殼模組；
- 配置面固定以 RDB 為事實來源，未來只替換有價值的運行面端口。

## 2. 目標與非目標

### 2.1 目標

- 支援任意租戶、任意業務對象類型，且全鏈路身份不混淆；
- 支援類型化屬性、人工標籤、計算標籤、組合分群、互斥集合和預設標籤；
- 支援 AND、OR、NOT、數值區間及標籤依賴；
- 規則可草擬、校驗、版本化發佈、全量重算和安全回退；
- 屬性事件、重算任務和物化寫入具備冪等、重試和崩潰恢復能力；
- 可獨立部署為 `kudos-ms-tag`，也可將 core 嵌入其他服務；
- API、模組、Ktorm、Flyway、快取、測試和命名符合 Kudos 習慣；
- 為未來 ClickHouse/Kafka/Flink 保留清晰端口，但不讓首版依賴它們。

### 2.2 首版非目標

- 使用者臨時提交任意 Attribute 查詢 DSL；
- ClickHouse、Kafka、Flink 或其他大資料基礎設施；
- 機器學習標籤與任意 Kotlin/Java 腳本規則；
- 跨租戶共享運行資料；
- 通用前端規則編輯器；
- 將 Subject 的完整業務主資料複製到標籤庫；
- 無限制大小的同步重算；
- 以 RDB 方案解決超大規模、亞秒級全量切換問題。

## 3. 方案選擇

### 3.1 採用：`kudos-ms-sys` 同構 + core 內部端口

七個標準模組提供統一部署模型，core 內以端口隔離計算與運行存儲。首版端口都有可工作的 JVM/RDB 實現，未來重型模組只替換運行端口。

優點是最符合 Kudos 現況、接入成本最低、沒有空殼模組，且本地/遠程契約天然一致。

### 3.2 未採用：先抽成 `kudos-ability-tag`

標籤具有獨立的配置、資料、API、管理面和任務生命週期，更接近原子服務而非純橫切能力。先建立 ability kernel 再包一層 ms facade 會造成兩套模組邊界和契約歸屬問題。

### 3.3 未採用：首版建立完整 adapter 矩陣

預先建立 ClickHouse/Flink/Kafka 模組容易重演 Soul「聲明有實現、實際只有 Configuration/README」的問題。端口保留即可，模組應在真正落地時新增。

## 4. 核心領域模型

### 4.1 Subject：被打標對象

```kotlin
data class TagSubjectKey(
    val tenantId: String,
    val subjectType: String,
    val subjectId: String,
)
```

示例：

```text
tenant-a / estate.house / house-1001
tenant-a / game.game    / game-1001
tenant-a / hr.person    / person-1001
```

不變量：

- 三個字段在所有運行資料中非空；
- `subjectType` 是預先登記的命名空間式穩定代碼，不是終端使用者任意輸入；
- 完整 SubjectKey 必須出現在 API、DAO、事件、快取鍵、任務鍵與資料庫鍵；
- displayName/profile 只作展示，不參與身份與規則條件。

### 4.2 Attribute：類型化屬性

`TagAttributeDefinition` 定義某一 SubjectType 可供規則使用的屬性。首版值類型：

```text
STRING, INTEGER, DECIMAL, BOOLEAN, DATE, DATETIME
```

基數：

```text
SINGLE, MULTIPLE
```

可用操作由 valueType 和 cardinality 決定，不把操作集合保存成 CSV：

- SET：替換單值；
- ADD：數字增量；
- APPEND：增加多值；
- REMOVE：刪除指定多值；
- CLEAR：清空屬性。

資料入口統一為：

```kotlin
data class TagAttributeFact(
    val eventId: String,
    val subjectKey: TagSubjectKey,
    val attributeCode: String,
    val operation: AttributeOperation,
    val value: TagValue?,
    val occurredAt: Instant,
    val sourceCode: String,
    val sourceVersion: Long?,
)
```

`eventId` 用於冪等；`sourceVersion` 用於 SET/CLEAR 等非交換操作的亂序保護。沒有 sourceVersion 時按服務接收順序處理，契約標記為弱順序保證。ADD 依靠 eventId 達成 exactly-once effect。

### 4.3 Tag：穩定語義標籤

`TagDefinition` 只描述標籤本身：tenantId、subjectType、code、name、description、active、builtIn、version 等。

不建立 Soul 式 `NORMAL/GROUP/DEFAULT/MANUAL` 混合類型：

- 是否計算產生：看是否存在 published rule；
- 是否允許人工賦予：由 manualAssignable 控制；
- 是否為組合分群：規則是否引用其他 Tag；
- 是否為預設：由 TagSet.defaultTagId 表達；
- 是否互斥：由 TagSet.cardinality 表達；
- 賦值來源：保存在 Membership.sourceType。

`code` 是不可變業務鍵，`name` 是可修改顯示名稱。跨服務引用 id 或 code，不引用 name。

### 4.4 Segment：具備規則的物化 Tag

Segment 不建立另一套結果模型。具備已發佈規則且結果被物化的 Tag 即為可重用分群。

```text
estate.target_house =
    bedrooms = 3
    AND area >= 100
    AND (has_terrace = true OR has_rooftop = true)
```

完成計算後仍然形成普通 Assignment：

```text
house-1001 -> estate.target_house
```

### 4.5 Rule：版本化表達式樹

```kotlin
sealed interface TagRuleExpression

data class AllOf(val children: List<TagRuleExpression>) : TagRuleExpression
data class AnyOf(val children: List<TagRuleExpression>) : TagRuleExpression
data class Not(val child: TagRuleExpression) : TagRuleExpression
data class AttributePredicate(
    val attributeCode: String,
    val operator: AttributeOperator,
    val operands: List<TagValue>,
) : TagRuleExpression
data class HasTag(val tagCode: String) : TagRuleExpression
```

首版 operator：

```text
EQ, NE, GT, GTE, LT, LTE, BETWEEN,
IN, NOT_IN, CONTAINS, EXISTS, NOT_EXISTS
```

已發佈 RuleVersion 不可原地修改。`HasTag` 只能引用同 tenant、同 subjectType 的 Tag，發佈時建立 dependency DAG 並檢測循環。Job 和 Membership 必須攜帶 ruleVersion，舊任務不能覆蓋新版本。

### 4.6 Membership 與 Assignment

Membership 是來源級事實：

```text
subjectKey + tagId + sourceType + sourceRef
```

sourceType：

```text
RULE, MANUAL, IMPORT, DEFAULT
```

同一標籤可同時存在多個來源。移除 RULE Membership 不得刪除 MANUAL/IMPORT Membership。存在至少一個 active 且未過期的 Membership 時，最終 Assignment 有效。

首版不提供「人工強制禁止規則標籤」；未來如有明確需求，可增加 OVERRIDE policy，而不修改既有來源語義。

### 4.7 TagSet：互斥與預設

TagSet.cardinality：

```text
SINGLE, MULTIPLE
```

SINGLE 集合的確定性解析順序：

```text
MANUAL > RULE > IMPORT > DEFAULT
```

同來源類型有多個候選時，依 `TagDefinition.setPriority DESC`、`membershipVersion DESC`、`tagId ASC` 決定，禁止依賴資料庫未指定順序。

DEFAULT 只允許配置在 SINGLE TagSet。沒有任何有效非 DEFAULT Membership 時，Resolver 才讓 DEFAULT 形成有效 Assignment；其他來源消失後可自動恢復預設。

### 4.8 Query：只操作已物化 Tag

核心查詢：

```text
getTags(subjectKey)
findSubjects(subjectType, tagExpression, paging)
matches(subjectKey, tagExpression)
```

普通業務查詢的 expression 只包含 Tag：

```text
AllTags, AnyTags, NotTags
```

Attribute Rule 只能在管理面預先配置，不允許普通調用方臨時提交。

## 5. 模組與依賴

```text
kudos-ms-tag
├─ kudos-ms-tag-common
├─ kudos-ms-tag-sql
├─ kudos-ms-tag-core
├─ kudos-ms-tag-client
├─ kudos-ms-tag-api-public
├─ kudos-ms-tag-api-admin
└─ kudos-ms-tag-api-internal
```

```text
                 common
                ^      ^
                |      |
             client   sql
                         ^
                         |
                        core
                     ^   ^   ^
                     |   |   |
                  public admin internal
```

### 5.1 common

放置跨進程穩定契約：

```text
io.kudos.ms.tag.common
├─ subject
├─ attribute/api|enums|vo
├─ assignment/api|vo
├─ catalog/api|vo
├─ query/api|expression|vo
└─ rule/enums|expression
```

約束：

- API 接口以 `I...Api` 命名；
- 路徑只以方法級 `@GetExchange`/`@PostExchange` 聲明；
- 不放 Service、DAO、Spring Bean、Ktorm Entity 或具體計算器；
- HTTP DTO 不暴露 PO 或資料庫規則節點。

主要契約：

```text
ITagAttributeFactApi
ITagAssignmentApi
ITagQueryApi
ITagCatalogApi
```

所有批量寫入包含 requestId；每個 mutation/fact 另有 eventId。

### 5.2 sql

```text
resources/sql/tag
├─ h2
├─ mysql
└─ postgresql
```

- 三方言保持相同表、索引和約束語義；
- H2 只作測試/本地開發，不作生產設計最低公分母；
- 應用層生成 UUID，避免依賴三方言不同的 UUID default；
- 每個版本只表達一個清晰遷移，不維護與 Flyway 分離的全量建表腳本；
- core 以 `api(project(...-sql))` 引入遷移資源；
- CI 對三種方言跑完整遷移和 DAO contract test。

### 5.3 core

```text
io.kudos.ms.tag.core
├─ subject
├─ attribute
├─ definition
├─ rule
├─ assignment
├─ query
├─ recalculation
├─ runtime/engine|spi|rdb
└─ platform/init/TagAutoConfiguration
```

各業務域沿用 `model/po`、`model/table`、`dao`、`service/iservice`、`service/impl`、`api`、`cache`、`event`。

本地 API 實現為 `@Primary @Component`，實現 common API。配置面 DAO 固定使用 RDB；運行面端口為：

```text
AttributeStateStore
TagMembershipStore
TagAssignmentIndex
TagRuleEvaluator
RecalculationQueue
AttributeSourceProvider
```

首版默認實現：

```text
RdbAttributeStateStore
RdbTagMembershipStore
RdbTagAssignmentIndex
JvmTagRuleEvaluator
RdbRecalculationQueue
```

默認實現以 `@ConditionalOnMissingBean` 裝配。未來 adapter 實現運行端口；首版不建立其模組。

### 5.4 client

Proxy 只繼承 common API，不重複聲明方法。所有 proxy 統一註冊到 `tag` HTTP service group：

```yaml
spring:
  http:
    serviceclient:
      tag:
        base-url: lb://kudos-ms-tag
        connect-timeout: 2s
        read-timeout: 10s
```

Fallback 不得把故障偽裝成業務結果：

- matches 不返回普通 false；
- findSubjects 不返回普通空集合；
- 寫入不返回成功；
- 返回帶 `UNAVAILABLE` 的結構化狀態，或拋標準 `TagServiceUnavailableException`。

### 5.5 API 模組

`api-internal` 提供服務間 Fact 推送、人工標籤、查詢和同步小批重算，是遠程 client 的主要目標。

`api-admin` 提供 Attribute/Tag/TagSet/Rule 管理、規則發佈、Job 查看/重試/取消、統計與審計。

`api-public` 首版不暴露任意 Subject 搜索和屬性寫入，只保留標準部署邊界；將來按具體產品資源歸屬與授權設計公開接口。

### 5.6 嵌入與獨立部署

```text
嵌入：業務服務 -> tag-core -> 本地 @Primary common API 實現
遠程：業務服務 -> tag-client -> HTTP -> tag-api-internal -> tag-core
```

原則上業務服務選 core 或 client 其中之一。聚合部署中兩者同時存在時，本地 `@Primary` 實現優先。

## 6. RDB 資料模型

### 6.1 通用約定

- Kotlin ID 為 String，資料庫 ID 為 `char(36)`；
- tenantId 在所有租戶配置與運行資料中 NOT NULL；
- 時間使用 UTC `timestamp(6)`；
- 配置表包含 active、builtIn、審計欄位和樂觀 version；
- 運行表只保留必要事件、版本與處理時間；
- Tag DB 不對 sys_tenant 建跨原子服務外鍵，tenantId 是邏輯引用；
- Tag 服務內控制面關聯使用 FK、UNIQUE 和 CHECK；
- 平台預置以租戶初始化模板複製，不使用 tenantId NULL 表示全局行。

### 6.2 控制面表

#### tag_subject_type

```text
id PK, code UNIQUE, name, owner_service_code, description,
active, built_in, version, audit columns
```

SubjectType 是平台級登記資料；ownerServiceCode 用於限制可推送 Fact 的服務身份。

#### tag_attribute_definition

```text
id PK, tenant_id, subject_type, code, name, description,
value_type, cardinality, active, built_in, version, audit columns
UNIQUE(tenant_id, subject_type, code)
```

valueType/cardinality 均使用 CHECK。合法 operation 由領域校驗器推導。

#### tag_set

```text
id PK, tenant_id, subject_type, code, name, cardinality,
default_tag_id NULL, active, built_in, version, audit columns
UNIQUE(tenant_id, subject_type, code)
```

defaultTagId 必須指向同集合 Tag，且只允許 SINGLE 集合配置。
由於 tagSet 與 tagDefinition 互相引用，初建集合時 defaultTagId 可為空；Tag 建立完成後再以樂觀鎖更新。資料庫增加 `FK(default_tag_id) -> tag_definition`，Service 同時校驗 tenantId、subjectType 與 tagSetId 一致。

#### tag_definition

```text
id PK, tenant_id, subject_type, code, name, description,
tag_set_id NULL, set_priority, published_rule_id NULL,
manual_assignable, active, built_in, version, audit columns
UNIQUE(tenant_id, subject_type, code)
FK(tag_set_id) -> tag_set
FK(published_rule_id) -> tag_rule
```

#### tag_rule

```text
id PK, tenant_id, tag_id, rule_version, status,
root_node_id NULL, expression_version, checksum,
published_time, retired_time, audit columns
UNIQUE(tag_id, rule_version)
FK(tag_id) -> tag_definition
```

當前規則由 tagDefinition.publishedRuleId 唯一指向，避免依賴跨方言 partial unique index。
rootNodeId 在草稿初建時可為空，保存完節點後更新；資料庫增加 `FK(root_node_id) -> tag_rule_node`。這兩個循環引用都由 Flyway 先建表、後加約束完成。

#### tag_rule_node

```text
id PK, rule_id, parent_id NULL, node_kind, order_num,
attribute_id NULL, referenced_tag_id NULL, operator NULL
FK(rule_id) -> tag_rule ON DELETE CASCADE
FK(parent_id) -> tag_rule_node
INDEX(rule_id, parent_id, order_num)
```

nodeKind：ALL_OF、ANY_OF、NOT、ATTRIBUTE_PREDICATE、HAS_TAG。節點形狀與跨 tenant/subjectType 規則由發佈校驗器強制。

#### tag_rule_operand

```text
id PK, node_id, order_num, value_type,
string_value, integer_value, decimal_value,
boolean_value, date_value, datetime_value
UNIQUE(node_id, order_num)
FK(node_id) -> tag_rule_node ON DELETE CASCADE
```

只允許與 valueType 對應的一個值欄位非空。EQ/BETWEEN/IN/EXISTS 的 operand 數量由校驗器強制。

#### tag_rule_dependency

```text
id PK, tenant_id, rule_id, tag_id, dependency_type,
attribute_id NULL, referenced_tag_id NULL
UNIQUE(rule_id, dependency_type, attribute_id, referenced_tag_id)
INDEX(tenant_id, dependency_type, attribute_id)
INDEX(tenant_id, dependency_type, referenced_tag_id)
```

它是發佈時生成的依賴索引，用於增量調度、管理展示、循環檢測和拓撲排序。

#### tag_taxonomy_node / tag_taxonomy_tag

只保留一套標籤樹：taxonomy node 保存 tenantId、subjectType、parentId、code、name、orderNum；relation 保存 nodeId、tagId、orderNum 並做唯一約束。分類樹只負責管理與展示，不參與規則求值。

### 6.3 運行面表

#### tag_subject

```text
tenant_id, subject_type, subject_id,
display_name NULL, profile_json NULL,
state_version, first_seen_time, update_time
PRIMARY KEY(tenant_id, subject_type, subject_id)
```

profileJson 只作展示，不參與規則或 JSON 查詢。

#### tag_attribute_event

```text
event_id PK, request_id, payload_checksum,
tenant_id, subject_type, subject_id, attribute_id,
operation, source_code, source_version,
occurred_time, received_time, process_status,
error_code, error_message, typed value columns
```

相同 eventId + 相同 checksum 返回 DUPLICATE；相同 eventId + 不同 checksum 返回 IDEMPOTENCY_CONFLICT。

#### tag_attribute_state

```text
id PK, tenant_id, subject_type, subject_id, attribute_id,
value_key, value_type, typed value columns,
source_event_id, source_version, state_version,
effective_time, expire_time, update_time
UNIQUE(tenant_id, subject_type, subject_id, attribute_id, value_key)
```

SINGLE 的 valueKey 固定 `_single`；MULTIPLE 使用規範化值 SHA-256。建立對象反查索引和 `(tenant_id, subject_type, attribute_id, typed_value, subject_id)` 規則掃描索引。

#### tag_membership

```text
id PK, tenant_id, subject_type, subject_id, tag_id,
source_type, source_ref, active, rule_version,
effective_from, effective_until, membership_version,
source_event_id, update_time
UNIQUE(tenant_id, subject_type, subject_id, tag_id, source_type, source_ref)
```

#### tag_assignment

```text
tenant_id, subject_type, subject_id, tag_id,
exclusive_set_id NULL, assignment_version,
evaluated_rule_version NULL, materialized_time,
effective_from, effective_until, update_time
PRIMARY KEY(tenant_id, subject_type, subject_id, tag_id)
INDEX(tenant_id, subject_type, tag_id, subject_id)
UNIQUE(tenant_id, subject_type, subject_id, exclusive_set_id)
```

exclusiveSetId 只在 SINGLE TagSet 填入。NULL 可容納多個非互斥 Tag；非 NULL 唯一約束保證同一對象同一互斥集合最多一個 Assignment。查詢始終過濾 effectiveUntil，不能依賴清理任務準時執行。

evaluatedRuleVersion 表示該 Tag 最新一次規則求值所用版本，不代表 RULE 一定是當前有效來源；即使 MANUAL Membership 維持 Assignment，RULE 求值為 false 時仍可記錄已評估版本。沒有 published rule 的 Tag 為空。

#### tag_assignment_event

```text
event_id PK, tenant_id, subject_type, subject_id, tag_id,
operation, cause_type, cause_ref, assignment_version, occurred_time
```

作為審計與未來外部投影來源；未來 Kafka/ClickHouse adapter 消費這一語義事件，而不是侵入核心 Service。

#### tag_recalculation_candidate

```text
run_id, tenant_id, subject_type, subject_id, tag_id,
rule_version, evaluated_time
PRIMARY KEY(run_id, subject_type, subject_id, tag_id)
```

runId 等於對應 RULE_FULL_REBUILD job 的 id。只保存命中候選，未命中由缺席表示。REBUILDING 候選不對普通查詢可見。

#### tag_recalculation_job

```text
id PK, job_key UNIQUE, tenant_id, job_type,
tag_id, rule_version, subject_type, subject_id NULL,
cursor_subject_id NULL, status, priority,
requested_version, processed_version,
attempt_count, max_attempts, available_time,
lease_owner, lease_until, processed_count,
last_error_code, last_error_message,
create_time, start_time, complete_time, update_time, version
```

SUBJECT_INCREMENTAL 使用穩定 jobKey 並重用 job row：新 Fact 提升 requestedVersion 並將任務置為待處理；Worker 只有在 processedVersion 追上 requestedVersion 後才能完成。這避免永久唯一 dedupKey 把後續合法變更一併抑制。

RULE_FULL_REBUILD 的 jobKey 包含 tagId + ruleVersion，按 cursorSubjectId keyset 續跑。

狀態：

```text
PENDING -> RUNNING -> SUCCEEDED
                   -> RETRY_WAIT
                   -> FAILED
                   -> CANCELLED
```

索引：`(status, available_time, priority, id)`、`(lease_until, status)`。

## 7. 計算與一致性

### 7.1 Attribute Fact 事務

一個有效 Fact 的同一 RDB 事務完成：

```text
insert attribute_event (idempotency)
-> upsert subject
-> apply attribute_state
-> resolve affected rules from dependency
-> upsert/increment recalculation_job.requested_version
-> commit
```

批量接口為非原子批次，返回逐 item 狀態。先完成純校驗，再以有限 transaction slice 寫入；不可恢復錯誤只拒絕對應 item，資料庫級暫時錯誤把整個 slice 標為 retryable failure。

### 7.2 增量重算

Worker 讀取 subject 最新 Attribute State，計算指定 published rule。結果只更新該 RuleVersion 的 RULE Membership，再運行 Assignment Resolver。Tag Assignment 變化後，通過 TAG dependency 建立下游 Segment job；整體按 DAG 拓撲推進，不使用遞歸調用。

### 7.3 發佈與全量重算

```text
DRAFT -> VALIDATING -> REBUILDING -> PUBLISHED -> RETIRED
```

流程：

1. 校驗 AST、類型、operand、引用邊界；
2. 建 dependency DAG 並檢測循環；
3. 生成 canonical checksum；
4. 建 RULE_FULL_REBUILD job；
5. keyset 分頁掃描 Subject，寫 candidate；
6. 成功後在一個 RDB 事務中替換該 tag 的 RULE Membership、重建受影響 Assignment、切換 publishedRuleId 並完成 job；
7. 舊規則進入 RETIRED。

重算期間普通查詢繼續讀舊物化結果。任一階段失敗不切換，舊結果保持有效。首次發佈在完成前沒有該計算 Tag 的 Assignment。

### 7.4 同步與異步

不使用隱式 AUTO：

- `submitFacts`：持久化 Fact/State/Job 後返回，物化結果最終一致；
- `recalculateSubjectNow`：可信 internal API 或嵌入式本地調用，小批量同步重算。

首版限制：同步 subject <= 100；單 Subject 直接依賴規則 <= 200；禁止同步觸發 full rebuild。查詢返回 assignmentVersion、materializedAt、evaluatedRuleVersion。首版不提供跨請求等待 token。

### 7.5 Assignment Resolver

- 任一有效 Membership 可使非互斥 Assignment 生效；
- SINGLE TagSet 先鎖定 subject + set，再按來源優先級、setPriority、membershipVersion、tagId 決定唯一結果；
- Rule 只變更自己的 Membership；
- DEFAULT 僅在沒有有效非 DEFAULT 候選時生效；
- 每次有效結果變化寫 assignmentEvent。

## 8. 任務並發、錯誤與恢復

Worker 以 RDB 租約領取任務：

```text
SELECT eligible jobs FOR UPDATE SKIP LOCKED
-> set leaseOwner/leaseUntil
-> commit
-> execute
-> conditional update by id + version + leaseOwner
```

MySQL/PostgreSQL 使用原生 SKIP LOCKED；H2 測試實現保持相同行為契約。DAO 封裝方言差異，不讓 Service 拼接方言 SQL。

保障：

- 多實例正常情況不重複領取；
- 崩潰後租約過期可重領；
- 極端重複執行仍由 eventId、唯一鍵、ruleVersion 和樂觀 version 保持冪等；
- 舊 Worker 無法用過期 lease 覆蓋新 Worker；
- full rebuild 從 cursorSubjectId 續跑，不使用 offset pagination。

錯誤分為：

- NON_RETRYABLE：規則非法、類型不匹配、引用不存在、跨 tenant/subjectType、循環；
- RETRYABLE：短暫資料庫錯誤、死鎖、租約衝突、可恢復 SourceProvider 錯誤。

重試使用 exponential backoff、maxAttempts。超限進 FAILED，admin 可查看與重試。禁止捕獲異常後仍標記成功。

## 9. 快取

只快取低基數、不可變或版本化資料：TagDefinition、TagRuleVersion、CompiledTagRule、Dependency、TagSet、Taxonomy。

鍵包含 tenantId + id/code + version。不快取高基數 Assignment 搜索結果、Attribute State 全量、Job 列表或任意查詢結果。

已發佈 RuleVersion 不可變，所以 compiled cache 不需全局清理。Job 攜帶精確 ruleVersion；找不到指定版本必須失敗，不能改用最新版本。當前版本指針使用短 TTL 或 Kudos remote cache。快取失效只允許影響性能，不得影響計算正確性；禁止靜態全局 Spring bean lookup 型 Cache Handler。

## 10. AttributeSourceProvider 擴展

```kotlin
interface AttributeSourceProvider {
    val providerCode: String
    fun validate(config: AttributeSourceConfig): List<ValidationError>
    fun fetch(config: AttributeSourceConfig, cursor: SourceCursor?, limit: Int): AttributeSourceBatch
}
```

- DB/配置只引用 providerCode，不保存類名；
- Registry 啟動時檢查 providerCode 唯一；
- cursor 對 core 不透明；
- Provider 輸出標準 TagAttributeFact；
- 密鑰只保存 secret reference；
- 首版不提供內建 Provider，也不建立來源配置表；真正落地第一個拉取 Provider 時，再連同安全配置和 cursor schema 一起設計。

## 11. 安全與租戶隔離

- HTTP/安全上下文 tenantId 必須與 request tenantId 一致；
- 普通調用方不能替其他租戶寫 Fact；
- 根據 subjectType.ownerServiceCode 限制服務可寫的對象類型；
- 跨租戶操作只允許平台管理權限；
- 人工標籤記錄操作者、原因、requestId/eventId；
- 日誌不輸出完整屬性值、profile 或批量請求；
- SubjectId 默認記 hash；
- builtIn 配置不可刪除，只能按政策停用或建立租戶配置。

建議權限：

```text
tag:subject-type:view
tag:attribute:view|manage
tag:definition:view|manage
tag:rule:view|manage|publish
tag:assignment:view|manual
tag:job:view|retry|cancel
```

## 12. 可觀測性

Metrics：

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

結構化關聯字段：tenantId、requestId、eventId、jobId、runId、subjectType、subjectIdHash、tagId、ruleVersion。

健康檢查：RDB、Worker lease 活性、最老 Pending Job、Failed Job 數、所有 Published Rule 可編譯性、Flyway schema version。

## 13. 測試策略

### 13.1 純邏輯測試

- 所有 valueType/operator 組合與邊界；
- ALL/ANY/NOT 結構；
- BETWEEN 邊界；
- 循環檢測和拓撲排序；
- Membership 多來源隔離；
- SINGLE TagSet 決策和 DEFAULT 恢復。

### 13.2 DAO contract test

相同契約運行於 H2、MySQL Testcontainers、PostgreSQL Testcontainers：

- 完整 Flyway；
- unique/FK/check；
- Attribute State upsert；
- AND/OR/NOT 查詢；
- keyset pagination；
- job lease 競爭與過期重領；
- candidate promotion。

AND 查詢基準語義：

```sql
SELECT tenant_id, subject_type, subject_id
FROM tag_assignment
WHERE tenant_id = ?
  AND subject_type = ?
  AND tag_id IN (...)
  AND (effective_until IS NULL OR effective_until > CURRENT_TIMESTAMP)
GROUP BY tenant_id, subject_type, subject_id
HAVING COUNT(DISTINCT tag_id) = ?
ORDER BY subject_id
```

### 13.3 Service/併發測試

- eventId 重投與衝突；
- sourceVersion 亂序；
- 並發 Fact；
- Worker 崩潰與 lease 恢復；
- Job requestedVersion 在運行中再次提升；
- 舊 ruleVersion 不覆蓋新版本；
- full rebuild 失敗時仍讀舊結果；
- promotion 後新結果一次性可見。

### 13.4 API contract test

- common API 同時驗證本地 core 和遠程 interface client；
- fallback 不製造假陰性、假空集合或假成功；
- tenant 上下文不匹配時拒絕；
- 批量接口逐 item 狀態穩定。

### 13.5 場景驗收

```text
房產：三室 AND 面積 >= 100 AND (露臺 OR 天台)
遊戲：五子棋 AND 1v1 AND 在線
人事：年齡 BETWEEN 30 AND 40
```

每個場景驗證 Fact 更新、Segment 物化、依賴重算、AND/OR 搜索、人工/規則來源共存和規則版本切換。

## 14. 分階段落地

### 階段 1：領域契約與配置面

- 七模組骨架；
- SubjectType、Attribute、Tag、TagSet；
- Rule AST、校驗、版本與 dependency；
- 三方言控制面遷移。

### 階段 2：運行面

- Fact ingestion 與 Attribute State；
- JVM evaluator；
- Membership/Assignment/Resolver；
- AND/OR/NOT 查詢；
- 三方言運行面遷移。

### 階段 3：可靠任務

- 增量 job、lease、retry；
- full rebuild candidate 和原子 promotion；
- 過期處理、依賴級聯和 DEFAULT；
- 故障恢復與併發測試。

### 階段 4：API 與工程保障

- common/core/client/internal/admin 契約；
- 權限與租戶校驗；
- 版本化 cache；
- metrics、health、審計；
- 三方言與本地/遠程 contract tests。

## 15. 驗收標準

- 三個示例場景均能由管理配置建立規則並物化為 Tag；
- SubjectKey 在任意查詢和寫入路徑都不能省略 tenantId/subjectType；
- 重複 Fact 不重複改變 State，亂序 SET 不覆蓋較新版本；
- 多來源 Membership 不互相刪除；
- 同一 SINGLE TagSet 永遠最多一個有效 Assignment；
- 規則循環無法發佈；
- 全量重算失敗不影響舊版本查詢；
- Worker 崩潰後任務可恢復，過期 Worker 不能提交結果；
- client 故障不會被解釋成「未命中」或「寫入成功」；
- H2/MySQL/PostgreSQL 通過相同遷移與 DAO 契約；
- 嵌入 core 與遠程 client 對上層呈現同一 common API。

## 16. 後續演進規則

只有在真實容量或延遲指標證明 RDB 運行面不足時才引入重型 adapter。配置面、RuleVersion、SubjectKey、Fact、Membership 和 Assignment 語義保持不變；ClickHouse 只作 AssignmentIndex/分析讀模型，Kafka 只作可靠傳輸，Flink 只作 RuleEvaluator/流式計算實現。任何 adapter 都必須通過與 RDB/JVM 相同的端口 contract test，不能形成第二套業務語義。

# Branch onboarding: kudos-ms sys / user / auth / msg port

This branch (`claude/objective-diffie-ab9212`, 20 commits ahead of `main`) ports
functionality from the Java reference codebase `soul-ms-*` into the Kotlin
`kudos-ms-*` microservices, following kudos conventions (BaseCrudController,
Ktorm, Feign with method-level annotations, INotifyListener). Where soul's
design diverges from kudos's, we kept kudos's design and only ported the
functionality.

Four module families touched: **sys**, **user**, **auth**, **msg**. The biggest
new piece is the **dispatch pipeline** added to msg (template → publish → MQ →
channel listener → failure tracking).

The original gap-analysis was: *"compare soul-ms-sys/user/auth/msg with the
kudos counterparts, fill in what's missing without copying soul's design."*

---

## How the module layout works

Every `kudos-ms-<domain>` family has the same seven submodules:

```
kudos-ms-<domain>/
├── kudos-ms-<domain>-sql              # Flyway migrations (h2/pg)
├── kudos-ms-<domain>-common           # API interfaces + VOs (no Spring beans)
├── kudos-ms-<domain>-core             # entities, DAOs, services, local Api impls
├── kudos-ms-<domain>-api-admin        # /api/admin/** controllers (deployable)
├── kudos-ms-<domain>-api-internal     # /api/internal/** RPC controllers (deployable)
├── kudos-ms-<domain>-api-public       # /api/public/** controllers (deployable)
└── kudos-ms-<domain>-client           # Feign proxies for consumers
```

Consumers depend on `<domain>-client`. The three `api-*` modules are independent
Spring Boot applications (each has its own `@EnableKudos` entrypoint and
`*AutoConfiguration`).

---

## The `IXxxApi` pattern (the most important convention)

For every API surface that's both a) called locally inside the service and b)
exposed over HTTP to other services via Feign, we use **one interface, three
realizations** — driven by method-level HTTP annotations on the interface
itself.

```kotlin
// common/.../IAuthRoleApi.kt — single source of truth for both Feign and MVC paths
interface IAuthRoleApi {
    @GetMapping("/api/internal/auth/role/getRoleById")
    fun getRoleById(@RequestParam id: String): AuthRoleCacheEntry?
    // ...
}

// core/.../AuthRoleApi.kt — local in-JVM impl, marked @Primary
@Primary
@Service
open class AuthRoleApi : IAuthRoleApi { ... }

// api-internal/.../AuthRoleInternalController.kt — same interface; Spring MVC
// reads the annotations off the interface to register HTTP handlers.
@RestController
class AuthRoleInternalController(
    private val authRoleApi: AuthRoleApi,
) : IAuthRoleApi {
    override fun getRoleById(id: String): AuthRoleCacheEntry? = authRoleApi.getRoleById(id)
}

// client/.../IAuthRoleProxy.kt — Feign re-uses the same interface paths
@FeignClient(name = "auth-role", fallback = AuthRoleFallback::class)
interface IAuthRoleProxy : IAuthRoleApi
```

**Why `@Primary`:** both `AuthRoleApi` (the @Service bean) and the controller
class register as `IAuthRoleApi` beans. `@Primary` on the @Service bean
disambiguates so other services autowire the local impl, not the controller.

**Why annotations on the interface, not `@RequestMapping` at class level:**
type-level `@RequestMapping` would make the @Component impl get accidentally
registered as an MVC handler. Method-level annotations are inherited by
controllers but ignored by Spring MVC on non-controller beans.

Common modules pull `compileOnly("org.springframework:spring-web")` to compile
the annotations without dragging Spring into the runtime classpath of pure
client consumers.

**Where to add a new internal-RPC method:**

1. Add it to `IXxxApi.kt` in `common/...` with `@GetMapping`/`@PostMapping`.
2. Implement it in `XxxApi.kt` in `core/...` (the `@Primary @Service` class).
3. Add an `override` line in `XxxInternalController.kt` (delegating to `XxxApi`).
4. Add an `override` to `XxxFallback.kt` returning the safe default
   (`null` / `emptyList()` / `false`).
5. The Feign proxy needs no change — it inherits from `IXxxApi`.

---

## Admin controllers via `BaseCrudController`

Admin endpoints inherit free CRUD by extending the
`BaseCrudController<PK, Service, Query, Row, Detail, Edit, FormCreate, FormUpdate>`
generic. Per-domain custom endpoints sit alongside:

```kotlin
@RestController
@RequestMapping("/api/admin/auth/role")
class AuthRoleAdminController :
    BaseCrudController<String, IAuthRoleService, AuthRoleQuery, AuthRoleRow,
        AuthRoleDetail, AuthRoleEdit, AuthRoleFormCreate, AuthRoleFormUpdate>() {

    @PutMapping("/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean) =
        service.updateActive(id, active)

    @PostMapping("/bindUsers")
    fun bindUsers(@RequestParam roleId: String, @RequestBody userIds: Collection<String>) =
        authRoleUserService.batchBind(roleId, userIds)
}
```

This is how all 5 msg entities, 2 auth aggregate entities (role/group with
their join-table operations), and the user/sys controllers are wired.

---

## Domain tour

### sys
`SysOutLine`, `SysLocale` modules ported; common-module API interfaces got
HTTP annotations; admin/internal controllers reach feature parity with soul.
Nothing exotic.

### user
The biggest single module by surface area. Login flow + account safety:

- **`PassportService.login`** — full gate sequence: `active` → not frozen →
  password (BCrypt via `PasswordKit`) → OTP (if enabled) → success +
  last-login update. Statuses are an enum (`PassportLoginStatusEnum`), not
  HTTP codes — every failure still returns 200 with a typed result so the
  frontend can do differentiated UI.
- **Freeze columns** — `freeze_type / freeze_start_time / freeze_end_time /
  freeze_title / freeze_content / freeze_time`. Login refuses when
  `freeze_type IS NOT NULL` and now is inside the window. `AutoUnfreezeScheduler`
  runs hourly (cron configurable via `kudos.user.auto-unfreeze.cron`) and
  clears expired rows — pure hygiene, login already handles "expired but
  not cleaned" correctly.
- **OTP / TOTP** — `GoogleAuthenticator.verifyCode` had a non-RFC
  sign-extension bug fixed in `1110455d`. Setup endpoint returns
  `otpauth://` URL; the QR rendering lives in `BarcodeKit` (in
  `kudos-base`, uses only zxing-core + `java.awt.BufferedImage`).
- **`SessionUserPrincipal`** — `IIdEntity<String> + Serializable` snapshot
  stored in `HttpSession[KudosContext.SESSION_KEY_USER]`. The
  `UserContextWebFilter` (`@Order(LOWEST_PRECEDENCE - 100)`) lifts it into
  `KudosContextHolder.get().user` on every request.
- **`CurrentUserKit`** — small object with `currentUserIdOrNull()`,
  `currentPrincipalOrNull()`, `currentTenantIdOrNull()`, `currentUserId()`
  (the last throws if not logged in). Use this anywhere you need "who's
  calling" — it works in service code too, not just controllers.

### auth
Role/group with relation tables (role-resource, role-user, group-role,
group-user). The relation endpoints don't extend BaseCrudController because
"batch bind / unbind" is the natural verb, not single-row CRUD.

**`PermittedResourceApi.getMenusForCurrentUser`** — lives in
`api-public/...`, reads `CurrentUserKit` for the user id, fetches their
permitted resource ids via `authRoleService.getResources(userId)`, filters
to `ResourceTypeEnum.MENU`, builds a tree. **Orphan menus get promoted to
roots** (i.e. permitted child whose parent the user can't access) — that
avoids "ghost ancestor" hidden levels.

### msg
The previously-empty `IMsg*Api` interfaces are now load-bearing.

Inbox API (Batch from `300f16da`):

- `IMsgReceiveApi.getReceivesByUserId / getUnreadCountByUserId / markRead /
  markAllReadByUserId`
- `IMsgTemplateApi.getTemplateById / getTemplateByEvent(tenantId, eventType,
  msgType, locale?)`
- `IMsgInstanceApi.getInstanceById`

Status enums (`MsgReceiveStatusEnum`, `MsgSendStatusEnum`,
`MsgPublishMethodEnum`) keep the SQL dict codes in one place — change the
SQL, change the enum, change at most one Kotlin call site.

---

## The dispatch pipeline (4 commits, msg)

The headline addition. Soul's `NoticePublishHandler` + per-channel `MqConsumer`
+ `NoticeUnreceived` design, redrawn on kudos's `INotifyProducer` /
`INotifyListener` framework (which is itself a thin layer over Spring Cloud
Stream's `@MqProducer`/`@MqConsumer`).

**End-to-end:**

```
Business code → IMsgSendApi.publish(MsgPublishRequest)
       │
       ▼
MsgPublishService
   1. resolve template via (tenantId, eventType, msgType, locale?)
   2. render via MsgTemplateRenderer            ← Batch 1 (9b285e83)
   3. insert MsgInstance + MsgSend(PENDING)
   4. INotifyProducer.notify(MsgDispatchEvent)  ← Batch 2 (5dac4974)
   5. status → SENT_TO_MQ / FAILED_TO_SEND_TO_MQ
       │
       ▼
"mqNotify" topic (Spring Cloud Stream, Kafka/RabbitMQ/RocketMQ pluggable)
       │
       ▼
NotifyMqAutoConfiguration.mqNotify Consumer
   - deserializes NotifyMessageVo<JSONObject>
   - looks up INotifyListener by (namespace, notifyType)
       │
       ▼
MsgEmailDispatchListener (notifyType = "msg.dispatch.email")  ← Batch 3 (e4cc6cbb)
   - status → CONSUMED_FROM_MQ
   - lookup receivers' email contact_way (dict code "201")
   - NO_CONTACT receivers → MsgUnreceived row (NO_CONTACT)    ← Batch 4 (ae01d8cb)
   - EmailHandler.send → async callback
       - successes → MsgReceive(RECEIVED) per user
       - failures → MsgUnreceived row (CHANNEL_REJECT)
   - status → SUCCESS / SUCCESS_PARTIAL / FAILED_FINAL + successCount/failCount
```

**Why split this way:**

- `MsgTemplateRenderer` is `@Component`, no MQ deps — unit-tested in
  isolation (`MsgTemplateRendererTest`, 6 cases).
- `MsgPublishService` injects `INotifyProducer` via `ObjectProvider` so
  msg-core compiles even without notify-mq on the classpath; the producer
  only resolves at runtime when a deployment pulls in
  `kudos-ability-distributed-notify-mq`.
- `MsgEmailDispatchListener` is `@ConditionalOnProperty(name = "server-host")`
  — no SMTP config means no bean means no listener registered. The framework
  logs "无 listener 配置" and drops the event, instead of crashing on startup.
- `MsgDispatchEvent` carries `renderedTitle` / `renderedContent` (not
  `templateId`) — in-flight messages aren't affected if the template gets
  edited between produce and consume.
- `MsgUnreceived` is per-failed-receiver, not aggregated. Same `(sendId,
  receiverId)` can have multiple rows — each retry attempt gets its own
  audit trail. `bumpRetry` records the attempt; the actual re-publish is up
  to the caller (manual admin retry today; a scheduler can be added).

**To turn the pipeline on in a deployment:**

1. Depend on `kudos-ability-distributed-notify-mq` (provides
   `INotifyProducer` and the `mqNotify` consumer).
2. Depend on `kudos-ability-comm-email` (already transitively via
   `msg-core`).
3. Configure SMTP:
   ```yaml
   kudos.msg.email:
     server-host: smtp.qq.com
     server-port: 465
     sender-account: notify@your.app
     sender-password: <app-token>
     from-mail-address: notify@your.app
     ssl: true
   ```
4. Insert a `msg_template` row for your event with `title`/`content`
   placeholders. Then call `msgSendApi.publish(MsgPublishRequest(...))`.

**Known-deliberate gaps:**

- Email is the only channel implemented. SMS / WebSocket / siteMsg listeners
  follow the same pattern (new `@Component` implementing `INotifyListener`,
  `notifyType()` returns the matching `MsgPublishMethodEnum.listenerType`).
- SMTP config is yml-driven (single-tenant). Soul's per-tenant
  `NoticeEmailInterface` table isn't ported.
- Receiver expansion (role/dept/online-users) isn't implemented;
  `MsgPublishRequest.receiverIds` must be a concrete user-id list. The
  `MsgSend.receiver_group_type_dict_code` column is hard-coded to `"user"`.
- No retry scheduler. `MsgUnreceived.bumpRetry` exists; nothing calls it on
  a timer.

---

## Practical recipes

### "Where do I add a new admin endpoint?"
Add to the appropriate `*AdminController` in `<domain>-api-admin/...`. If
it's a standard CRUD operation, the inherited `BaseCrudController` already
gives you `/list`, `/get/{id}`, `/save`, `/update`, `/delete/{id}`. Custom
verbs ride alongside as `@GetMapping`/`@PostMapping`/`@PutMapping` methods.

### "Where do I add a new method exposed via Feign?"
1. Method on `IXxxApi` in `common/...` with HTTP annotation.
2. Override in `XxxApi` (`@Primary @Service`) in `core/...`.
3. Override in `XxxInternalController` in `api-internal/...`.
4. Override in `XxxFallback` in `client/...` returning the safe default.
That's it; the Feign client proxy inherits from `IXxxApi` and needs no
edits.

### "Who is the current user?"
```kotlin
val userId = CurrentUserKit.currentUserIdOrNull()
   ?: throw NotLoggedInException()  // or your domain error
```
Or `CurrentUserKit.currentUserId()` if a missing user is exceptional and
you want the throw built in. Both work in service code thanks to the
`UserContextWebFilter` pushing into `KudosContextHolder` on the request
thread.

### "How do I send a notification?"
```kotlin
@Resource lateinit var msgSendApi: IMsgSendApi  // local @Primary bean

msgSendApi.publish(
    MsgPublishRequest(
        tenantId = currentTenant,
        eventTypeDictCode = "order_paid",
        msgTypeDictCode = "notify",
        publishMethod = MsgPublishMethodEnum.EMAIL,
        receiverIds = setOf(buyerUserId),
        params = mapOf("orderId" to order.id, "amount" to order.amount.toString()),
    )
)
```
A template row keyed on (tenantId, event_type=order_paid, msg_type=notify,
locale) must exist; `${orderId}` and `${amount}` will be substituted. Auto
params (`${time}`, `${date}`, `${year}`/`${month}`/`${day}`) are added
automatically — business params win when names collide.

### "How do I add a new send channel?"
1. Add a new enum value to `MsgPublishMethodEnum` + a matching
   `publish_method` dict-item SQL row.
2. New `@Component` in `<your-channel>` package implementing
   `INotifyListener`. `notifyType()` returns
   `MsgPublishMethodEnum.YOUR_CHANNEL.listenerType`.
3. In `notifyProcess`, follow the email listener pattern: deserialize
   `MsgDispatchEvent`, push `MsgSend.status → CONSUMED_FROM_MQ`, attempt
   delivery, on completion call `msgSendService.finishSend(...)` with
   final status + counters, and `msgUnreceivedService.recordFailures(...)`
   for the failed-receiver subset.

### "How do I add a freeze/disable check to my new endpoint?"
The login path already enforces this; for individual endpoints behind
login, the user is already loaded into `KudosContextHolder`. Cross-check
freeze state via the cache entry on `KudosContextHolder.get().user` if
needed (it's the snapshot from login time; re-read via
`userAccountService.getUserRecord(id)` for a non-cached truth).

---

## Code that changed in `kudos-base` (worth knowing)

- **`PasswordKit`** (`io.kudos.base.security`) — BCrypt wrapper around
  Spring Security's `BCryptPasswordEncoder`. Cost 10 in prod, cost 4 in
  tests (passed explicitly via the second `hash` overload). Always go
  through this, not raw encoder calls.
- **`GoogleAuthenticator`** — RFC 6238 TOTP. The `verifyCode`
  truncation was non-RFC (didn't mask the high bit of `hash[offset]`);
  fixed in `1110455d`. If you ever roll your own TOTP, keep this in mind.
- **`BarcodeKit`** — `qrPngBytes(text, size = 250)` returns a PNG byte
  array, using only `zxing-core` + AWT + `ImageIO`. No `zxing-javase`
  dependency.
- **`CharSequence.fillTemplateByObjectMap`** — preexisting; `${name}`
  placeholder substitution used by `MsgTemplateRenderer`.

---

## What's *not* in this branch

- Frontend changes (this is server-only).
- New dict items beyond what `msg_unreceived` and the dispatch pipeline
  needed.
- Performance optimizations (caching layers beyond what existed).
- Integration tests for the full publish → consume → email flow — the
  consumer side requires a running MQ + SMTP, so it's outside the unit-test
  scope. The renderer has 6 unit tests; the listeners are wired to be
  testable but not yet covered.

---

## Pointers

- **For the dispatch pipeline architecture**: read commits 9b285e83
  → 5dac4974 → e4cc6cbb → ae01d8cb in order. Each commit message has a
  prose summary of *why*.
- **For the login flow**: `PassportService.login` plus its test
  `PassportServiceTest`.
- **For the `IXxxApi` pattern**: any one of `IAuthRoleApi`,
  `AuthRoleApi`, `AuthRoleInternalController`, `IAuthRoleProxy`,
  `AuthRoleFallback` — they're all small and parallel.
- **For session/auth bridging**: `UserContextWebFilter` +
  `SessionUserPrincipal` + `CurrentUserKit`. ~50 lines each.

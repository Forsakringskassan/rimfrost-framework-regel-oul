# Plan — FKPOC-988: rimfrost-framework-regel-oul implementation

## Context

FKPOC-988 (subtask) implements the source code for `rimfrost-framework-regel-oul`.
The kravdefinition (FKPOC-982) is already merged (`docs/krav.md`). The repo currently
contains only `pom.xml`, `README.md`, and `docs/krav.md` — no `src/`.

**Guiding constraint from the user:** the OUL uppgift creation implementation must be
**copied** from one of the existing implementations as literally as possible, not
rewritten. The two candidate sources are near-identical:

1. **`rimfrost-framework-regel` (base, pre-komplettering-refactor)** — already extracted the
   OUL/persistence machinery into `RegelRequestHandlerBase` + `KompletteringOulHandler`
   + full storage layer (`CloudEventDataStorage`, `RegelCommonDataStorage`,
   `ProcessTopicInfoStorage` and their Panache implementations).
2. **`rimfrost-framework-regel-manuell` (pre-komplettering)** — older, less-refactored
   form. Same pattern embedded in `RegelManuellRequestHandler`.

**Chosen source: `rimfrost-framework-regel` (base).** Rationale:
- The OUL-related code is already isolated into reusable methods on
  `RegelRequestHandlerBase` and the `storage/` package — the extraction is largely
  a "move + rename package" job.
- `KompletteringOulHandler` demonstrates the intended consumer usage pattern.
- Storage entities, mappers, repositories, and the `RegelPhysicalNamingStrategy`
  (which applies the configurable table prefix) already exist and satisfy FROUL-PR-01.

**Sources — file paths for copy targets:**

| From (`rimfrost-framework-regel`) | To (`rimfrost-framework-regel-oul`) |
|---|---|
| `src/main/java/.../regel/storage/{CloudEventDataStorage,ProcessTopicInfoStorage,RegelCommonDataStorage}.java` | `src/main/java/.../regel/oul/storage/` |
| `src/main/java/.../regel/storage/entity/*` | `src/main/java/.../regel/oul/storage/entity/` |
| `src/main/java/.../regel/storage/internal/*` (all 12 files) | `src/main/java/.../regel/oul/storage/internal/` |
| `src/main/java/.../regel/logic/entity/CloudEventData.java` | `src/main/java/.../regel/oul/logic/entity/` |
| OUL-only methods extracted from `RegelRequestHandlerBase` | `src/main/java/.../regel/oul/logic/OulUppgiftService.java` (new) |
| `src/test/resources/db/migration/V002__cloud_event_data.sql`, `V003__common_data.sql`, `V004__process_topic_info.sql` | `src/main/resources/db/migration/V001..V003__*.sql` (renumbered, standalone) |

The OUL Kafka status listener is not present in base regel — it lives in
`rimfrost-framework-oul` (`OulKafkaConsumer` → `OulHandlerInterface.handleOulStatus()`).
Base regel implementations plug into this by implementing `OulHandlerInterface`.
This framework will provide a default implementation (`OulStatusListener`) that
performs the storage update + handläggning sync described in FROUL-FR-02.

---

## Design decisions

**D1. Consumer API shape — DECIDED: injectable `@ApplicationScoped` service.**
regel-oul publishes `OulUppgiftService` (and any collaborators) as CDI beans
that consumers inject. This matches the dominant rimfrost pattern:
`KompletteringOulHandler`, `KompletteringService`, `RegelKafkaProducer`,
`OulAdapter`, `HandlaggningAdapter`, and all `*Adapter` beans across the
framework repos. Consumers (`regel-manuell`, `regel-komplettering`) will call
`@Inject OulUppgiftService oulUppgiftService;` and invoke methods on it. No
abstract base class / inheritance.

**D2. Handläggning update scope — DECIDED: private helpers on `OulUppgiftService`.**
Handläggning update is not solely an OUL concern (maskinell and manuell
middleware also update handläggning), and the existing rimfrost pattern is
context-specific duplication rather than a shared service:
`RegelMaskinellRequestHandler` and `RegelManuellMiddlewareService` each carry
their own private `getHandlaggning` / `createHandlaggningUpdate` /
`updateHandlaggning` copies with different signatures. regel-oul follows the
same pattern: it owns the two OUL-context flows (create-uppgift →
update handläggning; OUL status → update handläggning) as private methods on
`OulUppgiftService`, copied from `RegelManuellRequestHandler` lines 110–114
and 281–301. No shared `HandlaggningUppgiftService` bean.

**D3. Config key naming — DECIDED: keep as-is.**
`regel.persistence.table-prefix` (table-prefix, FROUL-PR-01.2) and
`kafka.subtopic` (OUL reply subtopic) keep their existing names for
backward-compatibility with consumers (manuell, komplettering) that already
set them.

---

## Steps

Each step ends with `mvn spotless:apply && mvn test-compile -q` at minimum;
steps touching runtime code also end with `mvn test`.

### 1. Package skeleton and Maven wiring sanity-check

- Create empty package tree: `se.fk.rimfrost.framework.regel.oul` with
  subpackages `logic`, `logic/entity`, `storage`, `storage/entity`,
  `storage/internal`, `presentation/kafka` (for the OUL status listener).
- Add `src/main/resources/application.properties` with the JPA/Flyway defaults
  copied from base regel (datasource inactive by default; consumer must enable).
- Verify `mvn test-compile -q` succeeds against the empty source tree.

### 2. Copy persistence layer verbatim (entities, mappers, repositories, storage impls)

- Copy the 3 storage interfaces from base regel `storage/` → `regel/oul/storage/`
  (rename package only).
- Copy `storage/entity/{RegelCommonData,ProcessTopicInfo}.java` and
  `logic/entity/CloudEventData.java` → `regel/oul/{storage,logic}/entity/`.
- Copy `storage/internal/` (all 12 files: `Panache*Storage`, `*Entity`,
  `*Mapper`, `*Repository`, `RegelPhysicalNamingStrategy`) → `regel/oul/storage/internal/`.
- Update `RegelPhysicalNamingStrategy` if it reads a config key — verify it uses
  `regel.persistence.table-prefix` (per FROUL-PR-01.2).
- Add Flyway migrations to `src/main/resources/db/migration/`:
  `V001__cloud_event_data.sql`, `V002__common_data.sql`,
  `V003__process_topic_info.sql` (content copied from base regel's
  `src/test/resources/db/migration/V002..V004`).
- FROUL-PR-01.3 (reject startup if prefix missing) is satisfied by
  `RegelPhysicalNamingStrategy.toPhysicalTableName` throwing
  `IllegalStateException` when Hibernate resolves table names — no separate
  `@Startup` bean needed. Correctly stays silent for consumers who disable
  hibernate (`quarkus.hibernate-orm.enabled=false`) and thus don't use the
  storage at all.
- Naming strategy wiring: consumers set
  `quarkus.hibernate-orm.physical-naming-strategy=se.fk.rimfrost.framework.regel.oul.storage.internal.RegelPhysicalNamingStrategy`
  in their own `application.properties` (same pattern as regel-manuell today).
  Not defaulted in the framework's own properties to avoid imposing the prefix
  on consumer-owned entities.
- `mvn test-compile -q`.

### 3. Define the OUL SPI DTO — `OulUppgiftSpec`

**Q1 — DECIDED: skip `CloudEventDataFactory`.** Consumers build
`CloudEventData` themselves via `ImmutableCloudEventData.builder()` (Immutables
already generates a fluent builder from the existing `@Value.Immutable`
interface). Adding a framework factory would just be a thin wrapper with no
extra value; the builder is idiomatic and already public API. If a common
factory becomes useful across consumers later, it can be extracted then.

**Q2 — DECIDED: option (a) — spec carries both `cloudEventData` and
`cloudEventAttributes`.** `OulUppgiftSpec` accepts the typed
`CloudEventData` (used for persistence in `cloud_event_data`) AND a separate
`Map<String,String> cloudEventAttributes` (passed to OUL as-is). No derivation
between the two, no coupling to OUL's map shape inside the framework.

**`subTopic` resolution:** not a field on `OulUppgiftSpec`. `OulUppgiftService`
injects it internally via `@ConfigProperty(name = "kafka.subtopic")` (per D3),
matching how `RegelRequestHandlerBase` (line ~49) and `KompletteringOulHandler`
(line ~36) already resolve it today. Consumers set `kafka.subtopic=<value>` in
their own `application.properties` — unchanged from current behavior.

**`OulUppgiftSpec` fields** (Immutables `@Value.Immutable` interface):
- `handlaggningId : UUID`
- `replyTo : String`
- `cloudEventData : CloudEventData`
- `cloudEventAttributes : Map<String, String>`
- `regel : String`
- `beskrivning : String`
- `verksamhetslogik : String`
- `roll : String`
- `url : String`
- `erbjudande : String` (nullable — check base regel usage)

Steps:
- Create `se/fk/rimfrost/framework/regel/oul/logic/entity/OulUppgiftSpec.java`
  as `@Value.Immutable` interface.
- `mvn spotless:apply && mvn test-compile -q`.

### 4. Implement `OulUppgiftService` (per D1 option 1)

**Design sketch:**

```java
@ApplicationScoped
public class OulUppgiftService {
    @Inject OulAdapter oulAdapter;
    @Inject HandlaggningAdapter handlaggningAdapter;
    @Inject CloudEventDataStorage cloudEventDataStorage;
    @Inject ProcessTopicInfoStorage processTopicInfoStorage;
    @Inject RegelCommonDataStorage regelCommonDataStorage;
    @ConfigProperty(name = "kafka.subtopic") String oulReplyToSubTopic;

    /** FROUL-FR-01.1..01.8, FROUL-FR-03.1..03.4 — atomic-ish create + persist. */
    public OperativUppgift createOulUppgift(OulUppgiftSpec spec, CloudEventData ced);

    /** FROUL-FR-01.9 — best-effort end. */
    public void tryEndOulUppgift(UUID uppgiftId, String reason);

    /** FROUL-FR-03.5..03.6 — best-effort cleanup of all 3 stores. */
    public void cleanupCorrelation(UUID handlaggningId);
}
```

Implementation copies method bodies from `RegelRequestHandlerBase`
lines 136–162 (`tryEndOperativUppgift`, `createOperativUppgift`) and
290–417 (all `write*/read*/tryDelete*` methods), plus the orphan-cleanup
pattern from `KompletteringOulHandler` lines 91–111 (on storage failure after
OUL create, best-effort end the just-created OUL task).

- `mvn spotless:apply && mvn test`.

### 5. Implement handläggning-update as private helpers on `OulUppgiftService` (per D2)

- Copy from `RegelRequestHandlerBase` (lines 190–271) as private methods on
  `OulUppgiftService`: `getHandlaggning`, `updateHandlaggning`,
  `createHandlaggningUpdate`, `createUppgift`, `createUppgiftSpecifikation`,
  `toHandlaggningModelIdtyp`.
- Inject `HandlaggningAdapter` directly (no new shared bean — see D2).
- Wire the create-uppgift handläggning update into `createOulUppgift`
  (FROUL-FR-01.7), copying from `RegelManuellRequestHandler` lines 110–114.
- The status-update handläggning flow is wired in Step 6.
- `mvn spotless:apply && mvn test`.

### 6. Implement OUL status handling on `OulUppgiftService`

**Class layout — DECIDED: Option A (implement `OulHandlerInterface` directly on
`OulUppgiftService`).** No separate `OulStatusListener` class. The framework-oul
`OulMessageHandler` resolves the handler via `Instance<OulHandlerInterface>.get()`,
which requires exactly one implementation per application. This matches the only
existing production impl (`RegelManuellRequestHandler`) and keeps all
regel-oul-owned correlation state on a single bean.

**Error handling — DECIDED: Q1a-yes — full match to reference impl including
`sendErrorResponse`.** The catch block mirrors
`RegelManuellRequestHandler.handleOulStatus` lines 266–323: log error, build
`RegelErrorInformation`, `tryEndOulUppgift`, `cleanupCorrelation` (all 3 stores),
and `sendErrorResponse` to the reply topic. Reply topic comes from
`oulStatus.processInfo().replyTopic()` (available on framework-oul 1.1.1 — our
resolved dep).

Steps:
- Make `OulUppgiftService implements OulHandlerInterface`. Framework-oul's
  `OulMessageHandler` discovers it via CDI.
- Inject `RegelKafkaProducer` (from base regel jar, already on classpath) for
  `sendErrorResponse`. `RegelMapper` cannot be reused because it operates on
  base regel's `CloudEventData` type, not our copy — the response builder is
  inlined in `sendErrorResponse` using `ImmutableRegelResponse` + our
  `CloudEventData`.
- Add public `handleOulStatus(OulStatus oulStatus)`:
  - Read `RegelCommonData` by `handlaggningId`. If null → log info + return
    (FROUL-FR-02.5).
  - Read `CloudEventData` from storage.
  - `getHandlaggning(handlaggningId)`.
  - Build `updatedUppgift` = `.from(uppgift).version(uppgift.version()+1)
    .utforarId(toHandlaggningModelIdtyp(oulStatus.utforarId()))
    .planeradTs(oulStatus.planeradTill())
    .uppgiftStatus(oulStatus.uppgiftStatus())`.
  - `createHandlaggningUpdate` passing `handlaggning.version()` UNCHANGED
    (FROUL-FR-02.4).
  - `writeRegelCommonData` + `updateHandlaggning` (FROUL-FR-02.2).
- Catch `RuntimeException`:
  - Log error.
  - Build `RegelErrorInformation` (from `RegelCancelledException` if applicable,
    else `RegelFelkod.RIMFROST_OTHER`).
  - `tryEndOulUppgift(oulStatus.uppgiftId(), "Internal error")`.
  - `cleanupCorrelation(handlaggningId)` (deletes all 3 stores best-effort).
  - `sendErrorResponse` on `oulStatus.processInfo().replyTopic()`.
- New private helpers: `readRegelCommonData` (returns null on missing, throws
  `RegelCancelledException` on other errors — base regel lines 348–363);
  `sendErrorResponse` (base regel lines 234–246).
- Reference: `RegelManuellRequestHandler.handleOulStatus`
  (`regel-manuell/logic/RegelManuellRequestHandler.java` lines 266–323).
- `mvn spotless:apply && mvn test`.

### 7. Test infrastructure — aligned to rimfrost pattern

Aligned to how `rimfrost-framework-regel-manuell` structures its tests. The
framework's own tests must exercise the same wiring path consumers do:
`RegelPhysicalNamingStrategy` + prefixed tables + WireMock for HTTP adapters +
Postgres via Quarkus dev services.

- `src/test/resources/application.properties`:
  - `regel.persistence.table-prefix=regel_oul_test` and
    `quarkus.hibernate-orm.physical-naming-strategy=…RegelPhysicalNamingStrategy`
    — so tests hit the same naming strategy code path consumers hit in prod.
  - `quarkus.flyway.locations=classpath:db/migration/oul` — reroute Flyway to
    the test-side migration set whose table names already include the
    `regel_oul_test_` prefix. Main migrations at `src/main/resources/db/migration/`
    are unprefixed and belong to the consuming service, not the framework.
  - `quarkus.datasource.active=true`, `quarkus.hibernate-orm.enabled=true`,
    `quarkus.datasource.devservices.enabled=true` — Postgres via dev services.
  - `mp.messaging.outgoing.regel-responses.connector=smallrye-in-memory` — the
    Kafka producer used by `sendErrorResponse` is verified via in-memory sink.
  - `handlaggning.api.base-url` and `oul.api.base-url` set to a harmless
    fallback; overridden at test time by the WireMock resource's `start()` map.
- `src/test/java/.../helpers/WireMockRegelOul.java` — extends
  `WireMockHandlaggning` (from `rimfrost-framework-regel:1.4.1:tests`), adds
  OUL stubs (`POST /uppgifter`, `POST /uppgifter/.+/end`) and the
  `oul.api.base-url` mapping. Applied via
  `@QuarkusTestResource(WireMockRegelOul.class)`.
- `src/test/resources/db/migration/oul/V001__framework_tables.sql` — the three
  framework tables with the `regel_oul_test_` prefix baked in.
- `src/test/java/.../base/OulUppgiftServiceTestBase.java` — abstract
  `@TestInstance(PER_CLASS)` base injecting `EntityManager` and
  `@Connector("smallrye-in-memory") InMemoryConnector`; `@BeforeEach
  resetState()` truncates the three prefixed tables and clears the
  regel-responses sink.
- `src/test/java/.../helpers/OulTestData.java` — static factories
  (`handlaggning`, `cloudEventData`, `oulUppgiftSpec`, `oulStatus`, `erbjudande`).
- `pom.xml` `maven-jar-plugin` `test-jar` execution already includes
  `base/` and `helpers/` packages — no change needed.

### 8. Tests — one class per requirement group, `@DisplayName("FROUL-…")`

Per `~/.claude/CLAUDE.md`: every test gets an `@DisplayName` referencing the
FROUL requirement ID from `docs/krav.md`.

**Split into sub-steps** — each ends with `mvn spotless:apply && mvn test`
and a commit, so the pattern established in the first class can be adjusted
before it propagates to the rest.

#### Common setup for all sub-steps

All test classes:
- `@QuarkusTest`
- `@QuarkusTestResource(WireMockRegelOul.class)` — installs OUL POST stubs and
  handläggning GET/PUT stubs (see below).
- `extends OulUppgiftServiceTestBase` — provides `EntityManager` +
  `InMemoryConnector` + `resetState()` per test.
- `@Inject OulUppgiftService oulUppgiftService;`

Additional WireMock stubs needed beyond `WireMockRegelOul` today:
- `WireMockRegelOul` currently only stubs OUL `/uppgifter` POSTs. The
  handläggning GET/PUT stubs must be added — either as static JSON mapping
  files under `src/test/resources/mappings/` (rimfrost pattern, see
  `regel-manuell/src/test/resources/mappings/get-handlaggning-*.json`) or as
  in-code `server.stubFor(...)` calls in `WireMockRegelOul.start()`. Pick the
  in-code approach: keeps everything in Java, no separate JSON to sync with
  the `Handlaggning` model shape.

For error-path tests, override individual stubs per-test via
`WireMockRegelOul.getWireMockServer().stubFor(...)` at `atPriority(1)` (matches
how `RegelManuellDoneFaultHandlingTest` does it).

#### Sub-step 8a — `OulUppgiftServiceCreateTest`

Covers FROUL-FR-01.1..01.8, FROUL-FR-03.1..03.4.

Extend `WireMockRegelOul.start()` with handläggning GET/PUT stubs (dynamic:
match any `/handlaggning/{uuid}` GET and return a `Handlaggning` JSON derived
from the requested id). Verify via `EntityManager` that the three tables have
rows for the handläggning after `createOulUppgift` returns.

Test methods:
- `create_should_call_oul_createOperativUppgift` — FROUL-FR-01.1: verify
  a POST hit `/uppgifter` (via `WireMockRegelOul.waitForRequest`).
- `create_should_include_regel_beskrivning_verksamhet_roll_in_oul_request` —
  FROUL-FR-01.2: capture request body, assert JSON fields.
- `create_should_include_url_in_oul_request` — FROUL-FR-01.3.
- `create_should_include_cloudevent_attributes_in_oul_request_processInfo` —
  FROUL-FR-01.4.
- `create_should_include_reply_subtopic_from_kafka_subtopic_config` —
  FROUL-FR-01.5: assert `subTopic == "oul-test"` (from test app.properties).
- `create_should_persist_regel_common_data_with_uppgift_ids` — FROUL-FR-01.6.
- `create_should_send_put_handlaggning_with_uppgift_reference` —
  FROUL-FR-01.7: verify PUT body via `WireMockRegelOul.getLastPutHandlaggning`.
- `create_should_not_include_individer_in_oul_request` — FROUL-FR-01.8.
- `create_should_persist_cloud_event_data` — FROUL-FR-03.1.
- `create_should_persist_reply_topic_in_process_topic_info` —
  FROUL-FR-03.2, FROUL-FR-03.3.
- `create_should_persist_uppgift_and_oul_uppgift_id_in_common_data` —
  FROUL-FR-03.4.

##### Deviations from plan during 8a

- **Entity name collision with regel jar.** Indexing
  `rimfrost-framework-regel` under `quarkus.index-dependency.*` causes
  Hibernate to also discover the base regel jar's `CloudEventDataEntity`,
  `RegelCommonDataEntity`, and `ProcessTopicInfoEntity`. Because our copies
  used the same simple class names, `DuplicateMappingException` blocked
  startup. Fixed by giving our entities distinct JPA entity names via
  `@Entity(name = "Oul...")`. `@Table(name = ...)` unchanged since the tables
  really are separate.
- **Missing `regel_oul_test_komplettering_tillstand` table.** The regel jar
  also declares `KompletteringTillstandEntity`, so Hibernate expects the
  prefixed table to exist at startup. Added it to
  `V001__framework_tables.sql` (mirrors what a consumer service would ship).
- **Missing `RegelConfigProviderYaml` config.** The regel jar's YAML
  provider observer fires at startup and throws if `config.yaml` isn't on
  the classpath. Added `src/test/resources/config.yaml` based on the regel
  test jar's `config-test.yaml`.
- **Excluded RegelConsumer alongside RegelMessageHandler.** Excluding only
  `RegelMessageHandler` left `RegelConsumer` (its downstream) unsatisfied.
  Extended `quarkus.arc.exclude-types` to cover both.
- **WireMock response shape must match OUL/handläggning DTOs exactly.**
  Iterated to snake_case (`uppgift_id`, `handlaggning_id`, `process_info`,
  `cloudevent_attributes`, `reply_topic`, `sub_topic`) and added
  `underlag: []` to the handläggning response — the JAX-RS generated models
  have required creator properties that fail deserialization if missing.
- **Shared WireMock server needs per-test request-log reset.**
  `OulUppgiftServiceTestBase.resetState()` now calls
  `WireMockRegelOul.getWireMockServer().resetRequests()` so count-based
  assertions like `hasSize(1)` don't pick up requests from earlier tests.

#### Sub-step 8b — `OulUppgiftServiceStatusTest`

Covers FROUL-FR-02.2..02.5. FROUL-FR-02.1 (Kafka subscription) is
framework-oul's responsibility — verify only that `OulUppgiftService` is
resolvable as an `OulHandlerInterface` via CDI (one-liner).

Pre-seed the three tables via `regelCommonDataStorage.setRegelCommonData(...)`
etc., then invoke `handleOulStatus` directly (no Kafka consumer test needed
here — the consumer is tested in framework-oul).

Test methods:
- `status_should_update_stored_uppgift_status_utforar_planerad` —
  FROUL-FR-02.2: assert new `RegelCommonData` row has incremented version and
  new field values.
- `status_should_sync_to_handlaggning_via_put` — FROUL-FR-02.3.
- `status_should_not_increment_handlaggning_version` — FROUL-FR-02.4:
  capture PUT body, assert `handlaggning.version == originalVersion`.
- `status_should_be_ignored_when_no_regel_common_data` — FROUL-FR-02.5: no
  DB row seeded; assert no OUL end call, no regel-response sent, no PUT on
  handlaggning.
- `oulUppgiftService_should_be_registered_as_oulHandlerInterface_cdi_bean` —
  FROUL-FR-02.1 partial verification (implementation contract).

##### Deviations from plan during 8b

- `WireMockRegelOul` had to split the handläggning JSON into two payloads:
  the GET stub returns a body without `underlag` (the generated
  `Handlaggning` DTO used by the GET adapter path rejects unknown fields),
  while the PUT stub keeps `underlag: []` (the `HandlaggningUpdate` DTO in
  the PUT response requires it).
- The CDI verification for FROUL-FR-02.1 was simplified to an
  `@Inject OulHandlerInterface oulHandler;` field plus `isSameAs` assertion
  against the injected `OulUppgiftService`, rather than an explicit
  `CDI.current().select(...)` lookup.

#### Sub-step 8c — `OulUppgiftServiceEndAndCleanupTest`

Covers FROUL-FR-01.9, FROUL-FR-03.5, FROUL-FR-03.6.

Test methods:
- `tryEndOulUppgift_should_call_oul_endOperativUppgift_with_reason` —
  FROUL-FR-01.9.
- `tryEndOulUppgift_should_swallow_OulException` — FROUL-FR-01.9
  robustness (best-effort).
- `cleanupCorrelation_should_delete_all_three_stores` — FROUL-FR-03.5:
  pre-seed all 3 tables, invoke `cleanupCorrelation`, assert all empty.
- `cleanupCorrelation_should_continue_when_one_delete_fails` —
  FROUL-FR-03.6: use `@InjectMock` on one storage to throw; assert the other
  two still get deleted.

##### Deviations from plan during 8c

- Split into two test classes:
  `OulUppgiftServiceEndAndCleanupTest` (three tests: both FROUL-FR-01.9
  scenarios + FROUL-FR-03.5 happy path) and
  `OulUppgiftServiceCleanupResilienceTest` (one test: FROUL-FR-03.6).
  Reason: `@InjectMock` swaps a bean class-wide, so a class-scoped
  `@InjectMock ProcessTopicInfoStorage` would break the happy-path test
  that must seed and read through the real JPA implementation.
- `@InjectMock OulAdapter` is used for both FROUL-FR-01.9 tests so the
  end-call is verified with `Mockito.verify(...)` rather than by parsing
  the WireMock request body.

#### Sub-step 8d — `PersistenceStartupTest` + `OptimisticLockingTest`

Covers FROUL-PR-01.1, FROUL-PR-01.2, FROUL-PR-01.4, FROUL-NFR-01.1.

- `PersistenceStartupTest`:
  - `flyway_should_create_three_prefixed_tables` — FROUL-PR-01.1: query
    `information_schema.tables` in `regel_oul_test` schema, assert the three
    `regel_oul_test_*` tables exist.
  - `boot_should_apply_migrations_via_flyway` — FROUL-PR-01.4: implicit —
    if the tables exist after boot, Flyway ran.
- `OptimisticLockingTest` — FROUL-NFR-01.1:
  - `concurrent_writes_to_same_regel_common_data_should_reject_stale_write`
    — read entity in two transactions, write from each; second should throw
    `OptimisticLockException`.

**FROUL-PR-01.3 (reject startup if prefix missing)** — NOT automated. Boot
failure isolation inside `@QuarkusTest` is fragile (would kill the whole
Surefire JVM). Mark as manually verified against a consumer that forgets to
set the prefix; document in `README.md`.

Copy test bodies from `RegelRequestHandlerBaseTest`,
`KompletteringOulHandlerTest`, `KompletteringStorageTest` in base regel where
they overlap.

##### Deviations from plan during 8d

- Both tests placed in the `storage.internal` package (not just under
  `storage/`) so `OptimisticLockingTest` can read/write
  `RegelCommonDataEntity`'s package-private fields directly. The storage's
  public `setRegelCommonData` refreshes the version on every write, which
  would mask the optimistic-locking behaviour under test.
- `flyway_should_create_three_prefixed_tables` covers FROUL-PR-01.2
  implicitly: the prefix asserted is the test-config value
  (`regel.persistence.table-prefix=regel_oul_test`), demonstrating the
  prefix is configuration-driven.
- `boot_should_apply_migrations_via_flyway` asserts the `V001` row in
  `flyway_schema_history` with `success = true` (stronger than the
  "tables exist ⇒ Flyway ran" shortcut sketched in the plan).
- Concurrency is simulated single-threaded using three sequential
  `QuarkusTransaction.requiringNew()` blocks (seed → load+detach →
  writer B commits → stale merge). Avoids threading in the test while
  still exercising the `@Version` check on the DB round-trip.

### 9. Final verification

- `mvn spotless:apply && mvn test`.
- `mvn install -DskipTests` (verify the test-jar publishes).
- Read `krav.md` end-to-end and check each requirement ID has a corresponding
  `@DisplayName` in a test.
- Update `README.md` with a "How to consume" section (bean names to inject,
  required config keys, migration ordering caveat if consumer adds its own
  migrations).

---

## Out of scope for this ticket

- Migrating `rimfrost-framework-regel-manuell` and
  `rimfrost-framework-regel-komplettering` to consume this new framework —
  separate follow-up tickets.
- Removing the OUL/persistence code from `rimfrost-framework-regel` base —
  will happen once all consumers migrate. This framework can coexist
  temporarily.

---

## Notes / risks

- **CDI collision risk:** if a consumer classpath contains both
  `rimfrost-framework-regel` (which registers its own `RegelCommonDataStorage`
  etc.) and `rimfrost-framework-regel-oul`, CDI will fail to resolve. Package
  rename to `regel.oul` avoids type collision but consumers must not depend on
  both simultaneously. Document this in README.
- **Migration numbering:** consumers may already have their own Flyway
  migrations starting from V001. Recommend documenting that consumers of
  regel-oul should place their migrations under a different `flyway.locations`
  path, or use a version prefix (e.g. `V1xxx__` for oul, `V2xxx__` for consumer).

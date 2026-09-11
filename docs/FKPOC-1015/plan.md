# Plan: FKPOC-1015 — Cancelled cleanup

## Background

When a BPMN timeout occurs the rule flow never completes, leaving orphaned data in storage
(correlation tables), an open OUL task, and any rule-specific state. The framework needs to
consume a Kafka cancelled event and perform best-effort cleanup — ending the OUL task and
clearing all persisted correlation data — while also giving rule implementations a hook to
clean up their own data first.

The solution follows the established consumer → handler → interface pattern already used in
`rimfrost-framework-regel` (`RegelConsumer` / `RegelMessageHandler` / `RegelRequestHandlerInterface`)
and `rimfrost-framework-oul` (`OulKafkaConsumer` / `OulMessageHandler` / `OulHandlerInterface`).
The key difference: `RegelOulCancelledHandler` is optional — rule services that need no custom cleanup
do not implement it.

---

## Steps

- [x] **1. `RegelOulCancelledHandler` interface**

  Covers: FROUL-FR-04.5, FROUL-FR-04.6

  New file:
  `src/main/java/se/fk/rimfrost/framework/regel/oul/logic/RegelOulCancelledHandler.java`

  ```java
  public interface RegelOulCancelledHandler {
      /** Called before the framework cleans up correlation data and the OUL task. */
      void handleCancelled(UUID handlaggningId);
  }
  ```

  Optional to implement — rule services that need custom cleanup implement this interface.

---

- [x] **2. `RegelOulCancelledDeserializer`**

  Covers: FROUL-FR-04.2

  `RegelOulCancelledMessagePayload` (CloudEvent envelope with `data.handlaggningId`) is generated
  from `rimfrost-framework-regel-oul-asyncapi` — no hand-written POJO needed. Only a deserializer
  is required, following the same pattern as `RegelRequestDeserializer`.

  New file:
  `src/main/java/se/fk/rimfrost/framework/regel/oul/presentation/kafka/RegelOulCancelledDeserializer.java`

---

- [x] **3. `RegelOulCancelledService`**

  Covers: FROUL-FR-04.3, FROUL-FR-04.4, FROUL-FR-04.5, FROUL-FR-04.6, FROUL-FR-04.7

  New file:
  `src/main/java/se/fk/rimfrost/framework/regel/oul/logic/RegelOulCancelledService.java`

---

- [x] **4. Tests for `RegelOulCancelledService`**

  Covers: FROUL-FR-04.3, FROUL-FR-04.4, FROUL-FR-04.5, FROUL-FR-04.6, FROUL-FR-04.7

  New test class:
  `src/test/java/se/fk/rimfrost/framework/regel/oul/logic/RegelOulCancelledServiceTest.java`

  **Test approach — plain Mockito (`@ExtendWith(MockitoExtension.class)`):**
  `RegelOulCancelledService` branches on `Instance<RegelOulCancelledHandler>.isUnsatisfied()`.
  Both `@QuarkusTest` and `@QuarkusComponentTest` share a single CDI container across all test
  classes in a run; a mock `RegelOulCancelledHandler` bean registered in one class leaks into
  sibling classes, making it impossible to test the handler-absent path from a separate class.
  Separate Quarkus profiles would technically work but add significant boilerplate for no real
  gain. Plain Mockito stubs `isUnsatisfied()` directly per scenario — the right tool when the
  class under test is a pure-logic bean with no direct DB or HTTP calls.

  Scenarios (each with `@DisplayName` referencing FROUL-FR-04.x):

  | Scenario | Requirement |
  |---|---|
  | No correlation data → event ignored, no OUL call, no cleanup | FROUL-FR-04.7 |
  | Correlation data present, no `RegelOulCancelledHandler` → OUL ended, data cleaned | FROUL-FR-04.3, FR-04.4 |
  | Correlation data present, `RegelOulCancelledHandler` present → handler called first | FROUL-FR-04.5 |
  | `RegelOulCancelledHandler` throws → OUL end and cleanup still run | FROUL-FR-04.6 |
  | `tryEndOulUppgift` throws → cleanup still runs | FROUL-FR-04.3 |
  | No OUL task ID on correlation data → skip OUL end, still clean up | FROUL-FR-04.3 |

---

- [x] **5. `RegelOulCancelledConsumer`**

  Covers: FROUL-FR-04.1

  New file:
  `src/main/java/se/fk/rimfrost/framework/regel/oul/presentation/kafka/RegelOulCancelledConsumer.java`

  ```java
  @ApplicationScoped
  public class RegelOulCancelledConsumer {

      @Inject RegelOulCancelledService cancelledService;

      @Incoming("regel-oul-cancelled")
      @Blocking
      public void onCancelled(RegelOulCancelledMessagePayload message) {
          cancelledService.handleCancelled(UUID.fromString(message.getData().getHandlaggningId()));
      }
  }
  ```

  Channel `regel-oul-cancelled` is bound to `kafka.cancelled.topic` via:
  ```properties
  mp.messaging.incoming.regel-oul-cancelled.topic=${kafka.cancelled.topic}
  mp.messaging.incoming.regel-oul-cancelled.connector=smallrye-kafka
  ```

---

- [x] **6. Configuration**

  Covers: FROUL-FR-04.1

  In `src/main/resources/application.properties`, add a comment block documenting that
  consumers must supply `kafka.cancelled.topic`.

  In `src/test/resources/application.properties`, wire the `regel-oul-cancelled` channel to the
  in-memory connector and exclude `RegelOulCancelledConsumer` from ARC in tests that don't need it.

# FKPOC-1020 — Expose correlation data through OulUppgiftService public API

## Context

Consumers of `rimfrost-framework-regel-oul` that need correlation data during their
done-flow currently inject the three storage interfaces (`RegelCommonDataStorage`,
`ProcessTopicInfoStorage`, `CloudEventDataStorage`) directly. This leaks internal
implementation details — those interfaces are part of regel-oul's persistence layer,
not its public contract.

The immediate consumer is `rimfrost-framework-regel-manuell`, which reads all three
in `RegelManuellRequestHandler.handleUppgiftDone` to retrieve the OUL uppgift ID,
the reply topic, and the CloudEvent envelope before sending the final Kafka response.

This fix adds a single read method to `OulUppgiftService` so consumers can depend
only on the service and remove all direct storage interface injections.

---

## Steps

### Step 1 — Add `OulCorrelationData` value type

Create `src/main/java/.../logic/entity/OulCorrelationData.java` as an Immutables
`@Value.Immutable` interface in the `logic/entity` package (alongside `OulUppgiftSpec`
and `CloudEventData`):

```java
@Value.Immutable
public interface OulCorrelationData
{
   UUID oulUppgiftId();
   Uppgift uppgift();
   String replyTopic();
   CloudEventData cloudEventData();
}
```

Fields match exactly what `RegelManuellRequestHandler.handleUppgiftDone` currently
reads from the three storage interfaces individually.

### Step 2 — Add `getCorrelationData` to `OulUppgiftService`

Add the public method:

```java
/**
 * Returns the correlation data written by {@link #createOulUppgift} for the given
 * handlaggning, bundling the three persistent correlation rows into a single value.
 * Returns {@code null} if any of the three rows is missing.
 *
 * @param handlaggningId the handlaggning UUID
 * @return the correlation data, or {@code null} if not found
 */
public OulCorrelationData getCorrelationData(UUID handlaggningId)
{
   var commonData     = regelCommonDataStorage.getRegelCommonData(handlaggningId);
   var processInfo    = processTopicInfoStorage.getProcessTopicInfo(handlaggningId);
   var cloudEventData = cloudEventDataStorage.getCloudEventData(handlaggningId);
   if (commonData == null || processInfo == null || cloudEventData == null)
   {
      return null;
   }
   return ImmutableOulCorrelationData.builder()
         .oulUppgiftId(commonData.oulUppgiftId())
         .uppgift(commonData.uppgift())
         .replyTopic(processInfo.replyTopic())
         .cloudEventData(cloudEventData)
         .build();
}
```

### Step 3 — Add tests

Add a test class `OulUppgiftServiceCorrelationReadTest` (following the naming
convention of existing test classes in the repo):

| Scenario | Expected result |
|----------|-----------------|
| All three rows present | Returns `OulCorrelationData` with correct field values |
| `RegelCommonData` missing | Returns `null` |
| `ProcessTopicInfo` missing | Returns `null` |
| `CloudEventData` missing | Returns `null` |

Tests use the real Panache/PostgreSQL devservices setup from `OulUppgiftServiceTestBase`.
State is reset between tests via the existing `resetState()` truncation.

### Step 4 — Build and verify

- `mvn spotless:apply`
- `mvn test`
- Confirm `OulCorrelationData` is exported in the public API (no package-private
  leakage)

---

## Out of scope

- Changes to `rimfrost-framework-regel-manuell` — tracked separately (follow-up ticket)
- Changes to storage interface implementations — no changes needed
- Changes to `createOulUppgift` or write paths — no changes needed

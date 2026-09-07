# Plan — FKPOC-1016: add `endOperativUppgift` (throw-on-failure variant)

## Context

Today `OulUppgiftService` only exposes `tryEndOulUppgift(UUID, String)` — a best-effort
end that swallows `OulException` and logs. That fits the internal orphan-cleanup and
status-callback error paths (FROUL-FR-01.9), but leaves consumers who want to
end an OUL uppgift as part of a normal regel flow without a way to detect and
react to failures.

FROUL-FR-01.10 adds a sibling operation `endOperativUppgift(UUID, String)` that
delegates to the same OUL adapter call but propagates `OulException` to the
caller.

## Design

**Signature (new public method on `OulUppgiftService`):**

```java
/**
 * Ends an OUL uppgift with the given reason. Unlike
 * {@link #tryEndOulUppgift(UUID, String)}, failures are propagated to the
 * caller so consumers can react (retry, error response, cancel run, ...).
 *
 * @param uppgiftId OUL uppgift id
 * @param reason    human-readable reason recorded on the OUL uppgift
 * @throws OulException if OUL rejects the end request or is unreachable
 */
public void endOulUppgift(UUID uppgiftId, String reason) throws OulException
{
    oulAdapter.endOperativUppgift(uppgiftId, reason);
}
```

**Naming.** Method name mirrors the existing `tryEndOulUppgift`, dropping the
`try` prefix — matches the convention (`try*` = swallow, no prefix = throw).
Krav uses `endOperativUppgift` to name the *operation*; the Java method is
named `endOulUppgift` for symmetry with `tryEndOulUppgift`.

**No refactor of `tryEndOulUppgift`.** Not making `tryEndOulUppgift` a
`catch`-wrapper around the new method — the internal callers
(`createOulUppgift` orphan cleanup, `handleOulStatus` error path) intentionally
want the swallow behavior and readability of a single method is worth more
than the ~3 lines saved.

**Internal call sites — unchanged.** Both existing internal callers
(`createOulUppgift` line 140, `handleOulStatus` line 248) should keep using
`tryEndOulUppgift`. This new method is exclusively for external consumers.

## Steps

1. Add `endOulUppgift(UUID, String)` to `OulUppgiftService` (public, throws
   `OulException`). Javadoc references FROUL-FR-01.10 and contrasts with
   `tryEndOulUppgift`.
2. Add test in `OulUppgiftServiceEndAndCleanupTest`:
   - `FROUL-FR-01.10: endOulUppgift kastar OulException vidare vid fel`
     — asserts the `OulException` propagates; delegation also covered via
     `verify()` on the same test (no separate delegation test needed).
3. Run `mvn spotless:apply` and `mvn test`. Report results.
4. Update `README.md` if it lists the public `OulUppgiftService` API surface
   (quick check — skip if not covered there).

## Out of scope

- Changing internal orphan-cleanup or status-callback error paths.
- New `RegelFelkod` codes — `OulException` already carries enough error-type
  information for consumers.
- Consumer-side adoption (this is a framework-only change; consumers pick it up
  when they need it).

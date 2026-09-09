# Plan — FKPOC-1024: Wrapper för unassign i rimfrost-framework-regel-oul

## Mål

Exponera `unassignOperativUppgift` från `OulAdapter` via `OulUppgiftService`, med samma
try/throw-dualitet som redan finns för end-operationerna.

## Steg

- [x] **1. Uppdatera krav.md**
  Lägg till FROUL-FR-01.12 (`tryUnassignOperativUppgift`) och FROUL-FR-01.13
  (`unassignOperativUppgift`).

- [x] **2. Lägg till wrapper-metoder i OulUppgiftService**
  Lägg till `tryUnassignOulUppgift(UUID uppgiftId)` och
  `unassignOulUppgift(UUID uppgiftId)` direkt efter `endOulUppgift`, analogt med
  try/end-paret.

  - `tryUnassignOulUppgift` — best-effort, sväljer `OulException`, loggar felet.
  - `unassignOulUppgift` — kastar `OulException` vidare; void (konsekvent med
    end-metoderna; returvärdet från adaptern kasseras).

- [x] **3. Tester i OulUppgiftServiceUnassignTest**
  Ny testklass som följer mönstret från `OulUppgiftServiceEndAndCleanupTest`:

  | Test | Krav |
  |------|------|
  | `tryUnassignOulUppgift_should_call_oul_unassignOperativUppgift()` | FROUL-FR-01.12 |
  | `tryUnassignOulUppgift_should_swallow_OulException()` | FROUL-FR-01.12 |
  | `unassignOulUppgift_should_call_oul_unassignOperativUppgift()` | FROUL-FR-01.13 |
  | `unassignOulUppgift_should_propagate_OulException()` | FROUL-FR-01.13 |

## Filer som berörs

| Fil | Förändring |
|-----|------------|
| `docs/krav.md` | FR-01.12–01.13 tillagda (klart) |
| `src/main/java/.../logic/OulUppgiftService.java` | Två nya publika metoder |
| `src/test/java/.../logic/OulUppgiftServiceUnassignTest.java` | Ny testklass (4 tester) |

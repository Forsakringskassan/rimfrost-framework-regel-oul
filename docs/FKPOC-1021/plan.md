# FKPOC-1021 — Ta bort duplikat av `CloudEventData` från `rimfrost-framework-regel-oul`

## Bakgrund

`rimfrost-framework-regel-oul` definierar en egen `CloudEventData`-interface
(`se.fk.rimfrost.framework.regel.oul.logic.entity.CloudEventData`) som är strukturellt
identisk med den som redan finns i `rimfrost-framework-regel`
(`se.fk.rimfrost.framework.regel.logic.entity.CloudEventData`).

Eftersom `rimfrost-framework-regel` redan är ett compile-beroende i detta repo är
duplikaten ett förbiseende. Att ta bort den gör typgränsen tydligare och möjliggör
förenkling av `rimfrost-framework-regel-manuell` (se separat plan där).

## Påverkade filer i detta repo

**Produktion:**
- `CloudEventData.java` — tas bort
- `OulUppgiftSpec.java` — uppdatera import
- `OulCorrelationData.java` — uppdatera import
- `OulUppgiftService.java` — uppdatera import + fullt kvalificerade referenser
- `CloudEventAttributesMapper.java` — uppdatera import
- `CloudEventDataStorage.java` — uppdatera import
- `PanacheCloudEventDataStorage.java` — uppdatera import
- `CloudEventDataMapper.java` — uppdatera import

**Test:**
- `OulTestData.java` — uppdatera import
- `OulUppgiftServiceCreateTest.java` — uppdatera import
- `OulUppgiftServiceStatusTest.java` — uppdatera import
- `OulUppgiftServiceCorrelationReadTest.java` — uppdatera import
- `OulUppgiftServiceEndAndCleanupTest.java` — uppdatera import
- `OulUppgiftServiceCleanupResilienceTest.java` — uppdatera import

---

## Steg

- [ ] **1. Byt alla importer av `CloudEventData` till `rimfrost-framework-regel`-versionen**
  - Ersätt `se.fk.rimfrost.framework.regel.oul.logic.entity.CloudEventData` med
    `se.fk.rimfrost.framework.regel.logic.entity.CloudEventData` i alla produktions- och testfiler
  - Ersätt `ImmutableCloudEventData` med `ImmutableCloudEventData` från `rimfrost-framework-regel`
    (samma klass, annat paket)

- [ ] **2. Ta bort `CloudEventData.java`**
  - Ta bort `src/main/java/se/fk/rimfrost/framework/regel/oul/logic/entity/CloudEventData.java`

- [ ] **3. Bygg och testa**
  - `mvn spotless:apply`
  - `mvn test`

- [ ] **4. Releasa ny version**
  - Ny patch-version som konsumenter (bl.a. `rimfrost-framework-regel-manuell`) kan uppdatera till

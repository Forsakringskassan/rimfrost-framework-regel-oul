# rimfrost-framework-regel-oul changelog

Changelog of rimfrost-framework-regel-oul.

## 0.2.0 (2026-10-30)

### Features

-  send aktivitetId and optional processvyUrl to OUL when creating uppgift (FKPOC-1160) ([7bcbb](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/7bcbbfa16acbc0f) LisaWedin_Ductus)  

### Bug Fixes

-  update pom.xml ([59a7e](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/59a7e932129697a) Nils Elveros)  
-  update workflows ([aada4](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/aada473067ec1b9) Nils Elveros)  
-  mark FROUL-FR-05.3 as untestable and drop name-based test (FKPOC-1160) ([56ea8](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/56ea83d7f5e889f) LisaWedin_Ductus)  
-  åtgärda review-kommentarer (FKPOC-1160) ([6f5e8](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/6f5e8c31ce17ea3) LisaWedin_Ductus)  
-  **deps**  update dependency org.immutables:value to v2.12.3 ([6adf3](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/6adf3924fb27958) renovate[bot])  
-  Add storage failure fault tests for updateUppgift ([94d3a](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/94d3a41ff9e6914) Ulf Slunga)  
-  Add HandlaggningException fault tests for updateUppgift ([f2d3f](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/f2d3f77674b79fa) Ulf Slunga)  
-  bypass RegelCancelledException wrappers in updateUppgift storage calls ([eb00d](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/eb00d1823f7323d) Ulf Slunga)  
-  simplify updateUppgift to call adapter directly without CloudEventData ([d1e21](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/d1e2119f9621cf1) Ulf Slunga)  
-  adapt to handlaggning-adapter 1.2.5 API changes and add updateUppgift operation ([eeac4](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/eeac4a61c84caf1) Ulf Slunga)  
-  **deps**  update dependency org.yaml:snakeyaml to v2.7 ([66cb8](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/66cb8c7f76b47d9) renovate[bot])  
-  Add support for assignable flag from OUL adapter ([019d3](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/019d3a06d073b77) Lars Persson)  
-  **deps**  update dependency se.fk.rimfrost.framework.oul:rimfrost-framework-oul to v1.1.3 ([c9abb](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/c9abbd8c82279db) renovate[bot])  

### Dependency updates

- update dependency org.immutables:value-processor to v2.12.3 ([39725](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/397252463658a15) renovate[bot])  
- update forsakringskassan/.github digest to cbd74f1 ([9ee4b](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/9ee4bceda3f89dd) renovate[bot])  
- update dependency org.apache.maven.plugins:maven-compiler-plugin to v3.16.0 ([3dee6](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/3dee67bdd5c900c) renovate[bot])  
- update dependency io.smallrye:jandex-maven-plugin to v3.6.0 ([e4410](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/e4410eb9095394d) renovate[bot])  
- pin forsakringskassan/.github action to bfb01f7 ([a9ab6](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/a9ab63dd0de59a1) renovate[bot])  
### Other changes

**Merge branch 'main' into feat/FKPOC-1160-process-info-processvy**


[dbae8](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/dbae854ac0dec85) NilsElveros *2026-10-30 13:34:42*

**Merge branch 'main' into fix/FKPOC-1121-add-assignable-flag**


[63d9b](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/63d9b7160624b21) NilsElveros *2026-10-30 13:27:45*


## rimfrost-1_2 (2026-09-22)

## 0.1.2 (2026-09-22)

### Bug Fixes

-  handle null utforarId in handleOulStatus for unassign case ([99103](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/99103c3e3703aa7) Ulf Slunga)  
-  bump rimfrost-framework-regel-oul-asyncapi to 0.0.2, rimfrost-framework-regel to 1.4.4, rimfrost-framework-oul-adapter to 1.1.5 ([ecc2f](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/ecc2f162367b4ad) Ulf Slunga)  

## 0.1.1 (2026-09-18)

### Bug Fixes

-  Bump rimfrost-framework-oul version ([1fb86](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/1fb868467da0e8e) Lars Persson)  

## 0.1.0 (2026-09-14)

### Features

-  Add cancelled-event cleanup (FROUL-FR-04) ([26210](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/262107eaa13568e) Ulf Slunga)  

### Bug Fixes

-  Encapsulate OUL adapter types within OUL framework ([6549d](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/6549db208f357f6) Lars Persson)  
-  Remove bundled DB migrations — table creation delegated to consumers ([897aa](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/897aa966b780452) Ulf Slunga)  

### Dependency updates

- add renovate.json ([42e4c](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/42e4cc668b11954) renovate[bot])  
## 0.0.5 (2026-09-09)

### Bug Fixes

-  Add tryUnassignOulUppgift and unassignOulUppgift wrappers ([8dadd](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/8dadd9fbbc8f4e8) Ulf Slunga)  

## 0.0.4 (2026-09-08)

### Bug Fixes

-  Remove duplicate CloudEventData, use rimfrost-framework-regel type ([4e8d6](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/4e8d60cb78d29d7) Ulf Slunga)  

## 0.0.3 (2026-09-08)

### Bug Fixes

-  Truncate skapadTs to micros in seedUppgift to match PostgreSQL precision ([1d0a2](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/1d0a284b71c508e) Ulf Slunga)  
-  Add OulCorrelationData and getCorrelationData (FROUL-FR-01.11) ([c7ab8](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/c7ab85693815986) Ulf Slunga)  

## 0.0.2 (2026-09-07)

### Bug Fixes

-  Add endOulUppgift propagating OulException (FROUL-FR-01.10) ([64f7a](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/64f7a6ac9fa47a6) Ulf Slunga)  

## 0.0.1 (2026-09-04)

### Features

-  Bump rimfrost-framework-regel to 1.4.2 ([aea0f](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/aea0f02dd3d75bf) Ulf Slunga)  
-  Remove komplettering_tillstand workaround table (gone from regel 1.4.2) ([6c55a](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/6c55af4dcaed8d6) Ulf Slunga)  
-  Add default oul.api.base-url to application.properties ([48eb3](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/48eb32dcc46e546) Ulf Slunga)  
-  Update FKPOC-988 plan to reflect final implementation ([f5e54](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/f5e54754644274b) Ulf Slunga)  
-  Fix OffsetDateTime nanosecond/microsecond precision mismatch in status test ([b3657](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/b3657954f90d4b3) Ulf Slunga)  
-  Implement OulUppgiftService with correlation persistence and OUL integration ([38af3](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/38af3083a9f794f) Ulf Slunga)  
-  Remove redundant pom.xml properties ([79b6e](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/79b6ef236c3fe76) Ulf Slunga)  
-  Scaffold regel-oul framework with requirements and build ([066a7](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/066a769fd81ddf3) Ulf Slunga)  

### Bug Fixes

-  Restore quarkus.build.skip to prevent container image build ([e9af6](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/e9af6f00dd6e3b3) Ulf Slunga)  

### Other changes

**Update README.md**

* Co-authored-by: larpersso &lt;254442132+larpersso@users.noreply.github.com&gt; 

[d096c](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/d096c2f74e61614) Ulf Slunga *2026-09-04 12:45:45*

**Update .mvn/wrapper/maven-wrapper.properties**

* Co-authored-by: larpersso &lt;254442132+larpersso@users.noreply.github.com&gt; 

[f72cd](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/f72cd7465f96df3) Ulf Slunga *2026-09-02 12:59:30*

**Add files via upload**


[c6066](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/c60667c597bca9a) Ulf Slunga *2026-09-02 12:01:56*

**Create bundle-maven-lib-release.yaml**


[8beb2](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/8beb27c297b8ac9) Ulf Slunga *2026-09-02 11:59:59*

**Create bundle-maven-lib-ci.yaml**


[85eef](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/85eefff49e809c0) Ulf Slunga *2026-09-02 11:59:26*

**Create CODEOWNERS**


[96a5a](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul/commit/96a5a363f722fcb) Ulf Slunga *2026-09-02 11:58:40*



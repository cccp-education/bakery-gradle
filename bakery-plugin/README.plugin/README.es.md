<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — Internos del plugin

> Guía para desarrolladores y colaboradores del plugin Gradle `bakery-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **Versión**: `0.0.2` · **Group**: `education.ccp` · **ID del plugin**: `education.ccp.bakery`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Build**: `./gradlew build` · **Tests**: `./gradlew check` · **Puerta de cobertura**: `./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Distribución de módulos

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # Punto de entrada del plugin — registra todas las tareas (ramificación afterEvaluate)
│   │   ├── BakeryExtension.kt           # Extensión DSL (configPath, siteType, language, ia, themes, a11y, …)
│   │   ├── BakeryConstants.kt           # Grupos de tareas (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # Modelo site.yml (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (agregado)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # Accesibilidad WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille: segregación + enriquecimiento + presupuesto)
│   │   ├── article/ theme/ scaffold/   # DSLs de intención (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # Contratos N0 reflejados localmente (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # Pruebas funcionales GradleTestKit
├── src/e2eTest/kotlin/bakery/e2e/       # Pruebas E2E Playwright
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # Catálogo de versiones
```

## Contratos N0 (de workspace-bom MEMPHIS)

| Contrato | Artefacto | Proporciona |
|----------|----------|----------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(reflejado en `contracts/i18n/`)_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(reflejado en `contracts/pipeline/`)_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

El BOM se conecta vía `implementation(platform("education.cccp:workspace-bom:0.0.1"))` en
`bakery-plugin/build.gradle.kts:29`.

## Dependencias clave

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — motor de sitio estático (api)
- **Thymeleaf** `3.0.14.RELEASE` — renderizado de plantillas (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — despliegue gh-pages (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — diagramas AsciiDoc (api, en bundle jbake)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — servicio LLM BKY-IA-0
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — asíncrono para OllamaLlmService
- **Arrow-kt** (core + fx-coroutines) — programación funcional (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — análisis RSS (CS-8, reemplaza el frágil DocumentBuilderFactory DOM)
- **commons-io** `2.13.0` — utilidades de archivos (api)
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — pruebas de navegador E2E
- **Jackson** (kotlin + yaml) — config YAML, serialización JSON
- **Graphify plugin** `0.0.2` — integración de Knowledge Graph (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — consumidor de RAG / contexto aumentado

## Instancias Ollama (restricción global)

Los puertos `11434–11436` están prohibidos. Rotar sobre `11437–11465` (29 puertos).
Modelos autorizados: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Predeterminado de Bakery: `http://localhost:11464` + `gpt-oss:120b-cloud` (ver `IaConfig.kt`).

## Matriz de pruebas

| Tarea | SourceSet | Alcance | Timeout | Paralelismo |
|------|-----------|-------|---------|-------------|
| `test`           | test          | Unidad JUnit5 (excluye motor Cucumber + `bakery.scenarios.*`) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | Funcional GradleTestKit | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Navegador Playwright (depende de `installPlaywright`) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | Cobertura ≥ 85 % (analiza XML de Kover) | — | — |

Cableado de verificación (`build.gradle.kts`):
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` **no** está cableado en `check` — ejecutar explícitamente vía `./gradlew e2eTest`.
- Los pasos de Cucumber están en `src/test/scenarios`, features en `src/test/features`.
- `testImplementation` extiende de la implementación de `functionalTest` (no al revés).

## Ajuste JVM

- Todas las tareas `Test`: `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`: `maxHeapSize = "1g"`, limpia directorios temporales `gradle-test-*` obsoletos de más de 1h en `doLast`
- `e2eTest`: `maxParallelForks = 1`, `forkEvery = 1` (evitar conflictos de recursos del navegador)
- Kover: `includedSourceSets = ["main", "functionalTest"]`, reportes HTML + XML en `check`

## Comandos de build

```bash
./gradlew build                                   # build completo (UT + functionalTest + Cucumber)
./gradlew build -x test                            # solo compilar
./gradlew test                                     # pruebas unitarias JUnit5
./gradlew functionalTest                           # funcional GradleTestKit
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # Playwright E2E (auto-instalación Chromium)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # cobertura ≥ 85 %
./gradlew publishToMavenLocal                      # publicación local
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # instalar Chromium para E2E
```

> **Regla 0 (AGENT.adoc)**: `./gradlew -q publishToMavenLocal` es **obligatorio** tras cualquier cambio de código fuente,
> antes de cualquier prueba funcional desde un proyecto consumidor.

## Pipeline CI

`.github/workflows/test.yml` (job único, `ubuntu-latest`, timeout 20 min, JDK 24 temurin):
1. `./gradlew build` (UT + Cucumber) — en `bakery-plugin/`
2. `./gradlew installPlaywright` — instala Chromium
3. `./gradlew e2eTest` — Playwright E2E

Workflows auxiliares: `website.yml` (despliegue del sitio), `readme_plantuml.yml` (generación de README vía readme-gradle).

## Publicación (NMCP)

Configurado vía `com.gradleup.nmcp.settings` (`1.5.0`) en `bakery-plugin/settings.gradle.kts`.
Las credenciales se leen de `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`),
`publishingType = "AUTOMATIC"`.
El firmado usa `useGpgCmd()`; se omite el firmado cuando `CI=true` o la versión termina con `-SNAPSHOT`.
El POM (en `withType<MavenPublication>`) declara:
- Licencia Apache 2.0, desarrollador `cccp-education` (cccp.edu@gmail.com)
- SCM apuntando a `github.com/cccp-education/bakery-gradle`
- Inyección opcional de `relocationGroup` para migración de groupId

Bloque `gradlePlugin`: `id = education.cccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName "Bakery Plugin", etiquetas `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`.

Comando de publicación:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Estado de EPICs

Todos los EPICs cerrados (ver `bakery-plugin/.agents/INDEX.adoc`):
- EPICs 1–13 (gobernanza, Supabase→Firebase, build, formulario de contacto, timeout Cucumber, publishProfile,
  tolerancia YAML, release 0.1.4, scaffolding TDD, duración de tests, KG, cobertura) — ✅
- **BKY-JB-0→9** (experto JBake: scaffold, Thymeleaf, Google Forms, Firebase Auth, Analytics,
  theme, layouts, artículo IA, E2E) — ✅
- **BKY-LENS-1→6** (LENtille: segregación, enriquecimiento, presupuesto, reescritura BDD, conexión N3, plantillas aumentadas) — ✅
- **BKY-IA-2** (tema IA paramétrico) — ✅
- **BKY-KOV-1** (Kover 78 % → 85.88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, sesión 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, sesión 143) — ✅
- **BKY-COV-2** (microfisuras DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 idiomas, 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (migración real magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (Ley de la Economía de la Tinta, 3/3 pts) — ✅
- **BKY-CR-V5** (60 code smells) — ✅
- **BKY-CR-V6** (3 duplicaciones estructurales, 8/8 pts) — ✅
- **BKY-A11Y-1** (accesibilidad WCAG/RGAA, 13/13 pts) — ✅
- **BKY-FIX-1** (deployProfile sin site.yml) — ✅

## Documentos de arquitectura

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — Reglas absolutas y gobernanza
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — Sesiones, EPICs, roadmap
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — Trabajo restante
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — Misión de la sesión actual
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — Roadmap experto JBake
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — Revisión de código V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — Análisis de cobertura
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Patrón Lazy/Eager
- [README.adoc](../README.adoc) — README a nivel de proyecto (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## Contribuir

1. El build compila: `./gradlew build -x test`
2. Republicar localmente: `./gradlew -q publishToMavenLocal` (Regla 0, obligatorio)
3. Pruebas unitarias en verde: `./gradlew test`
4. Cucumber en verde: `./gradlew cucumberTest` (129/129)
5. Cobertura respetada: `./gradlew koverThresholdCheck` (≥ 85 %)
6. Seguir convenciones DDD (value objects, ports/adapters, Arrow `Either`/`Raise`, sin fugas)
7. Nunca sobrescribir `site.yml` sin backup (AGENT.adoc §1b — regla de seguridad)
8. Ejecutar el procedimiento de cierre de 6 pasos (AGENT.adoc §3) al final de la sesión

## Licencia

Apache License 2.0 — ver [LICENCE](../bakery-plugin/LICENCE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._
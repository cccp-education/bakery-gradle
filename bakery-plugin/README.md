<!-- master source — other languages are translations of this file -->
# bakery-gradle — Plugin Internals

> Developer & contributor guide for the `bakery-plugin` Gradle plugin.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **Version**: `0.0.2` · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.bakery`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Build**: `./gradlew build` · **Tests**: `./gradlew check` · **Coverage gate**: `./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: **EN** | [中文](README.plugin/README.zh.md) | [हिन्दी](README.plugin/README.hi.md) | [Español](README.plugin/README.es.md) | [Français](README.plugin/README.fr.md) | [العربية](README.plugin/README.ar.md) | [বাংলা](README.plugin/README.bn.md) | [Português](README.plugin/README.pt.md) | [Русский](README.plugin/README.ru.md) | [اردو](README.plugin/README.ur.md)

---

## Module layout

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # Plugin entry point — registers all tasks (afterEvaluate branching)
│   │   ├── BakeryExtension.kt           # DSL extension (configPath, siteType, language, ia, themes, a11y, …)
│   │   ├── BakeryConstants.kt           # Task groups (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # site.yml model (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (aggregate)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # Accessibility WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille: segregation + enrichment + budget)
│   │   ├── article/ theme/ scaffold/   # Intention DSLs (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # N0 contracts mirrored locally (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # GradleTestKit functional tests
├── src/e2eTest/kotlin/bakery/e2e/       # Playwright E2E tests
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # Version catalog
```

## N0 contracts (from workspace-bom MEMPHIS)

| Contract | Artifact | Provides |
|----------|----------|----------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(mirrored in `contracts/i18n/`)_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(mirrored in `contracts/pipeline/`)_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

The BOM is wired via `implementation(platform("education.cccp:workspace-bom:0.0.1"))` in
`bakery-plugin/build.gradle.kts:29`.

## Key dependencies

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — static site engine (api)
- **Thymeleaf** `3.0.14.RELEASE` — template rendering (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — gh-pages deployment (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — AsciiDoc diagrams (api, in jbake bundle)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — BKY-IA-0 LLM service
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — async for OllamaLlmService
- **Arrow-kt** (core + fx-coroutines) — functional programming (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — RSS parsing (CS-8, replaces fragile DocumentBuilderFactory DOM)
- **commons-io** `2.13.0` — file utilities (api)
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — E2E browser tests
- **Jackson** (kotlin + yaml) — YAML config, JSON serialization
- **Graphify plugin** `0.0.2` — Knowledge Graph integration (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — RAG / augmented context consumer

## Ollama instances (global constraint)

Ports `11434–11436` are forbidden. Rotate over `11437–11465` (29 ports).
Authorized models: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Bakery default: `http://localhost:11464` + `gpt-oss:120b-cloud` (see `IaConfig.kt`).

## Test matrix

| Task | SourceSet | Scope | Timeout | Parallelism |
|------|-----------|-------|---------|-------------|
| `test`           | test          | JUnit5 unit (excludes Cucumber engine + `bakery.scenarios.*`) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | GradleTestKit functional | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Playwright browser (depends on `installPlaywright`) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | Coverage ≥ 85 % (parses Kover XML) | — | — |

Verification wiring (`build.gradle.kts`):
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` is **not** wired into `check` — run explicitly via `./gradlew e2eTest`.
- Cucumber steps live in `src/test/scenarios`, features in `src/test/features`.
- `testImplementation` extends from `functionalTest` implementation (not the reverse).

## JVM tuning

- All `Test` tasks: `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`: `maxHeapSize = "1g"`, cleans stale `gradle-test-*` temp dirs older than 1h in `doLast`
- `e2eTest`: `maxParallelForks = 1`, `forkEvery = 1` (avoid browser resource conflicts)
- Kover: `includedSourceSets = ["main", "functionalTest"]`, HTML + XML reports on `check`

## Build commands

```bash
./gradlew build                                   # full build (UT + functionalTest + Cucumber)
./gradlew build -x test                            # compile only
./gradlew test                                     # JUnit5 unit tests
./gradlew functionalTest                           # GradleTestKit functional
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # Playwright E2E (Chromium auto-install)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # coverage ≥ 85 %
./gradlew publishToMavenLocal                      # local publish
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # install Chromium for E2E
```

> **Rule 0 (AGENT.adoc)**: `./gradlew -q publishToMavenLocal` is **mandatory** after any source change,
> before any functional test from a consumer project.

## CI pipeline

`.github/workflows/test.yml` (single job, `ubuntu-latest`, timeout 20 min, JDK 24 temurin):
1. `./gradlew build` (UT + Cucumber) — in `bakery-plugin/`
2. `./gradlew installPlaywright` — installs Chromium
3. `./gradlew e2eTest` — Playwright E2E

Auxiliary workflows: `website.yml` (site deploy), `readme_plantuml.yml` (README generation via readme-gradle).

## Publication (NMCP)

Configured via `com.gradleup.nmcp.settings` (`1.5.0`) in `bakery-plugin/settings.gradle.kts`.
Credentials read from `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`),
`publishingType = "AUTOMATIC"`.
Signing uses `useGpgCmd()`; signing skipped when `CI=true` or version ends with `-SNAPSHOT`.
POM (on `withType<MavenPublication>`) declares:
- Apache 2.0 license, developer `cccp-education` (cccp.edu@gmail.com)
- SCM pointing to `github.com/cccp-education/bakery-gradle`
- Optional `relocationGroup` injection for groupId migration

`gradlePlugin` block: `id = education.cccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName "Bakery Plugin", tags `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`.

Publication command:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## EPIC status

All EPICs closed (see `bakery-plugin/.agents/INDEX.adoc`):
- EPICs 1–13 (governance, Supabase→Firebase, build, contact form, Cucumber timeout, publishProfile,
  YAML tolerance, 0.1.4 release, scaffolding TDD, test duration, KG, coverage) — ✅
- **BKY-JB-0→9** (JBake expert: scaffold, Thymeleaf, Google Forms, Firebase Auth, Analytics,
  theme, layouts, AI article, E2E) — ✅
- **BKY-LENS-1→6** (LENtille: segregation, enrichment, budget, BDD rewrite, N3 connect, augmented templates) — ✅
- **BKY-IA-2** (parametric IA theme) — ✅
- **BKY-KOV-1** (Kover 78 % → 85.88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, session 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, session 143) — ✅
- **BKY-COV-2** (microfissures DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 languages, 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (real migration magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (Loi de l'Économie d'Encre, 3/3 pts) — ✅
- **BKY-CR-V5** (60 code smells) — ✅
- **BKY-CR-V6** (3 structural duplications, 8/8 pts) — ✅
- **BKY-A11Y-1** (WCAG/RGAA accessibility, 13/13 pts) — ✅
- **BKY-FIX-1** (deployProfile without site.yml) — ✅

## Architecture docs

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — Absolute rules & governance
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — Sessions, EPICs, roadmap
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — Remaining work
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — Current session mission
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — JBake expert roadmap
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — Code review V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — Coverage analysis
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Lazy/Eager pattern
- [README.adoc](../README.adoc) — Project-level README (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## Contributing

1. Build compiles: `./gradlew build -x test`
2. Republish locally: `./gradlew -q publishToMavenLocal` (Rule 0, mandatory)
3. Unit tests green: `./gradlew test`
4. Cucumber green: `./gradlew cucumberTest` (129/129)
5. Coverage respected: `./gradlew koverThresholdCheck` (≥ 85 %)
6. Follow DDD conventions (value objects, ports/adapters, Arrow `Either`/`Raise`, no leaks)
7. Never overwrite `site.yml` without backup (AGENT.adoc §1b — safety rule)
8. Run the 6-step closing procedure (AGENT.adoc §3) at session end

## License

Apache License 2.0 — see [LICENCE](../bakery-plugin/LICENCE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._
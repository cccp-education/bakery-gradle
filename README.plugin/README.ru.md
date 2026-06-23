<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — Внутреннее устройство плагина

> Руководство для разработчиков и контрибьюторов плагина Gradle `bakery-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **Версия**: `0.0.2` · **Группа**: `education.ccp` · **ID плагина**: `education.ccp.bakery`
- **Тулчейн**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Сборка**: `./gradlew build` · **Тесты**: `./gradlew check` · **Шлюз покрытия**: `./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Структура модулей

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # Точка входа плагина — регистрирует все задачи (ветвление afterEvaluate)
│   │   ├── BakeryExtension.kt           # DSL-расширение (configPath, siteType, language, ia, themes, a11y, …)
│   │   ├── BakeryConstants.kt           # Группы задач (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # Модель site.yml (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (агрегат)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # Доступность WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille: сегрегация + обогащение + бюджет)
│   │   ├── article/ theme/ scaffold/   # DSL намерений (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # Локально зеркалированные N0 контракты (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # Функциональные тесты GradleTestKit
├── src/e2eTest/kotlin/bakery/e2e/       # E2E тесты Playwright
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # Каталог версий
```

## N0 контракты (из workspace-bom MEMPHIS)

| Контракт | Артефакт | Предоставляет |
|----------|----------|----------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(зеркалировано в `contracts/i18n/`)_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(зеркалировано в `contracts/pipeline/`)_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

BOM подключён через `implementation(platform("education.cccp:workspace-bom:0.0.1"))` в
`bakery-plugin/build.gradle.kts:29`.

## Ключевые зависимости

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — движок статических сайтов (api)
- **Thymeleaf** `3.0.14.RELEASE` — рендеринг шаблонов (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — развёртывание gh-pages (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — диаграммы AsciiDoc (api, в jbake bundle)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — сервис LLM BKY-IA-0
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — асинхронность для OllamaLlmService
- **Arrow-kt** (core + fx-coroutines) — функциональное программирование (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — парсинг RSS (CS-8, заменяет хрупкий DocumentBuilderFactory DOM)
- **commons-io** `2.13.0` — файловые утилиты (api)
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — браузерные E2E тесты
- **Jackson** (kotlin + yaml) — конфиг YAML, JSON сериализация
- **Graphify plugin** `0.0.2` — интеграция Knowledge Graph (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — потребитель RAG / расширенного контекста

## Инстансы Ollama (глобальное ограничение)

Порты `11434–11436` запрещены. Ротация по `11437–11465` (29 портов).
Авторизованные модели: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
По умолчанию Bakery: `http://localhost:11464` + `gpt-oss:120b-cloud` (см. `IaConfig.kt`).

## Матрица тестов

| Задача | SourceSet | Область | Таймаут | Параллелизм |
|------|-----------|-------|---------|-------------|
| `test`           | test          | Модульные JUnit5 (исключает движок Cucumber + `bakery.scenarios.*`) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | Функциональные GradleTestKit | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Браузер Playwright (зависит от `installPlaywright`) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | Покрытие ≥ 85 % (парсит Kover XML) | — | — |

Подключение верификации (`build.gradle.kts`):
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` **не** подключён к `check` — запускать явно через `./gradlew e2eTest`.
- Шаги Cucumber в `src/test/scenarios`, features в `src/test/features`.
- `testImplementation` наследует implementation от `functionalTest` (не наоборот).

## JVM-тюнинг

- Все задачи `Test`: `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`: `maxHeapSize = "1g"`, очищает устаревшие `gradle-test-*` temp-директории старше 1h в `doLast`
- `e2eTest`: `maxParallelForks = 1`, `forkEvery = 1` (избегать конфликтов ресурсов браузера)
- Kover: `includedSourceSets = ["main", "functionalTest"]`, HTML + XML отчёты на `check`

## Команды сборки

```bash
./gradlew build                                   # полная сборка (UT + functionalTest + Cucumber)
./gradlew build -x test                            # только компиляция
./gradlew test                                     # модульные тесты JUnit5
./gradlew functionalTest                           # функциональные GradleTestKit
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # Playwright E2E (автоустановка Chromium)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # покрытие ≥ 85 %
./gradlew publishToMavenLocal                      # локальная публикация
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # установка Chromium для E2E
```

> **Правило 0 (AGENT.adoc)**: `./gradlew -q publishToMavenLocal` **обязателен** после любого изменения исходного кода,
> до любого функционального теста из проекта-потребителя.

## CI-пайплайн

`.github/workflows/test.yml` (одна job, `ubuntu-latest`, таймаут 20 min, JDK 24 temurin):
1. `./gradlew build` (UT + Cucumber) — в `bakery-plugin/`
2. `./gradlew installPlaywright` — устанавливает Chromium
3. `./gradlew e2eTest` — Playwright E2E

Вспомогательные workflows: `website.yml` (деплой сайта), `readme_plantuml.yml` (генерация README через readme-gradle).

## Публикация (NMCP)

Конфигурация через `com.gradleup.nmcp.settings` (`1.5.0`) в `bakery-plugin/settings.gradle.kts`.
Учётные данные читаются из `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`),
`publishingType = "AUTOMATIC"`.
Подпись использует `useGpgCmd()`; подпись пропускается при `CI=true` или версии, оканчивающейся на `-SNAPSHOT`.
POM (на `withType<MavenPublication>`) объявляет:
- Лицензия Apache 2.0, разработчик `cccp-education` (cccp.edu@gmail.com)
- SCM, указывающий на `github.com/cccp-education/bakery-gradle`
- Опциональная инъекция `relocationGroup` для миграции groupId

Блок `gradlePlugin`: `id = education.cccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName "Bakery Plugin", теги `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`.

Команда публикации:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Статус EPIC

Все EPIC закрыты (см. `bakery-plugin/.agents/INDEX.adoc`):
- EPICs 1–13 (управление, Supabase→Firebase, сборка, форма контакта, таймаут Cucumber, publishProfile,
  толерантность YAML, релиз 0.1.4, скаффолдинг TDD, длительность тестов, KG, покрытие) — ✅
- **BKY-JB-0→9** (эксперт JBake: скаффолд, Thymeleaf, Google Forms, Firebase Auth, Analytics,
  тема, макеты, AI-статья, E2E) — ✅
- **BKY-LENS-1→6** (LENtille: сегрегация, обогащение, бюджет, переписывание BDD, N3 connect, расширенные шаблоны) — ✅
- **BKY-IA-2** (параметрическая IA тема) — ✅
- **BKY-KOV-1** (Kover 78 % → 85.88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, сессия 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, сессия 143) — ✅
- **BKY-COV-2** (микротрещины DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 языков, 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (реальная миграция magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (Закон Экономии Чернил, 3/3 pts) — ✅
- **BKY-CR-V5** (60 code smells) — ✅
- **BKY-CR-V6** (3 структурных дубликата, 8/8 pts) — ✅
- **BKY-A11Y-1** (доступность WCAG/RGAA, 13/13 pts) — ✅
- **BKY-FIX-1** (deployProfile без site.yml) — ✅

## Документы по архитектуре

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — Абсолютные правила и управление
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — Сессии, EPIC, roadmap
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — Оставшаяся работа
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — Миссия текущей сессии
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — Roadmap эксперта JBake
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — Ревью кода V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — Анализ покрытия
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Паттерн Lazy/Eager
- [README.adoc](../README.adoc) — README уровня проекта (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## Контрибьюшн

1. Сборка компилируется: `./gradlew build -x test`
2. Локальная републикация: `./gradlew -q publishToMavenLocal` (Правило 0, обязательно)
3. Модульные тесты зелёные: `./gradlew test`
4. Cucumber зелёный: `./gradlew cucumberTest` (129/129)
5. Покрытие соблюдено: `./gradlew koverThresholdCheck` (≥ 85 %)
6. Следовать DDD-конвенциям (value objects, ports/adapters, Arrow `Either`/`Raise`, без утечек)
7. Никогда не перезаписывать `site.yml` без бэкапа (AGENT.adoc §1b — правило безопасности)
8. Запустить 6-шаговую процедуру закрытия (AGENT.adoc §3) в конце сессии

## Лицензия

Apache License 2.0 — см. [LICENCE](../bakery-plugin/LICENCE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._
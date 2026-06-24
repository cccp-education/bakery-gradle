<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — پلگ ان کی اندرونیات

> Gradle پلگ ان `bakery-plugin` کے لیے ڈویلپر اور تعاون کار گائیڈ۔

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **ورژن**: `0.0.2` · **گروپ**: `education.ccp` · **پلگ ان ID**: `education.ccp.bakery`
- **ٹول چین**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **بلڈ**: `./gradlew build` · **ٹیسٹ**: `./gradlew check` · **کوریج گیٹ**: `./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## ماڈیول لے آؤٹ

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # پلگ ان داخلہ نقطہ — تمام ٹاسک رجسٹر کرتا ہے (afterEvaluate برانچنگ)
│   │   ├── BakeryExtension.kt           # DSL ایکسٹینشن (configPath, siteType, language, ia, themes, a11y, …)
│   │   ├── BakeryConstants.kt           # ٹاسک گروپس (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # site.yml ماڈل (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (ایگریگیٹ)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # رسائی WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille: segregation + enrichment + budget)
│   │   ├── article/ theme/ scaffold/   # ارادہ DSLs (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # مقامی طور پر عکس N0 معاہدے (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # GradleTestKit فنکشنل ٹیسٹس
├── src/e2eTest/kotlin/bakery/e2e/       # Playwright E2E ٹیسٹس
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # ورژن کیٹلاگ
```

## N0 معاہدے (workspace-bom MEMPHIS سے)

| معاہدہ | آرٹیفیکٹ | فراہم کرتا ہے |
|----------|----------|----------|
| `codebase-contracts`  | `education.ccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.ccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(`contracts/i18n/` میں عکس)_ |
| `pipeline-contracts`  | `education.ccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(`contracts/pipeline/` میں عکس)_ |
| `agent-contracts`     | `education.ccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.ccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

BOM `bakery-plugin/build.gradle.kts:29` میں `implementation(platform("education.ccp:workspace-bom:0.0.1"))` کے ذریعے جڑا ہے۔

## اہم انحصارات

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — جامد سائٹ انجن (api)
- **Thymeleaf** `3.0.14.RELEASE` — ٹیمپلیٹ رینڈرنگ (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — gh-pages تعیناتی (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — AsciiDoc ڈایاگرام (api, jbake bundle میں)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — BKY-IA-0 LLM سروس
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — OllamaLlmService کے لیے غیر متزامن
- **Arrow-kt** (core + fx-coroutines) — فنکشنل پروگرامنگ (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — RSS پارسنگ (CS-8، ٹوٹلے DocumentBuilderFactory DOM کو بدلتا ہے)
- **commons-io** `2.13.0` — فائل یوٹیلیٹیز (api)
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — E2E براؤزر ٹیسٹس
- **Jackson** (kotlin + yaml) — YAML کنفیگ، JSON سیریلائزیشن
- **Graphify plugin** `0.0.2` — Knowledge Graph انضمام (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — RAG / augmented context کنزیومر

## Ollama انسٹنسز (عالمی پابندی)

پورٹس `11434–11436` ممنوعہ ہیں۔ `11437–11465` (29 پورٹس) پر گھومیں۔
اجازت یافتہ ماڈلز: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`۔
Bakery ڈیفالٹ: `http://localhost:11464` + `gpt-oss:120b-cloud` (`IaConfig.kt` دیکھیں)۔

## ٹیسٹ میٹرکس

| ٹاسک | SourceSet | دائرہ | ٹائم آؤٹ | متوازیت |
|------|-----------|-------|---------|-------------|
| `test`           | test          | JUnit5 یونٹ (Cucumber انجن + `bakery.scenarios.*` کو خارج کرتا ہے) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | GradleTestKit فنکشنل | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Playwright براؤزر (`installPlaywright` پر منحصر) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | کوریج ≥ 85 % (Kover XML پارس کرتا ہے) | — | — |

تصدیق وائرنگ (`build.gradle.kts`):
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` `check` میں **جڑا نہیں** ہے — واضح طور پر `./gradlew e2eTest` سے چلائیں۔
- Cucumber مراحل `src/test/scenarios` میں، features `src/test/features` میں۔
- `testImplementation` `functionalTest` implementation سے پھیلا ہے (الٹا نہیں)۔

## JVM ٹیوننگ

- تمام `Test` ٹاسکس: `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`: `maxHeapSize = "1g"`, `doLast` میں 1h سے پرانے سٹیل `gradle-test-*` ٹیمپ ڈائریکٹریز صاف کرتا ہے
- `e2eTest`: `maxParallelForks = 1`, `forkEvery = 1` (براؤزر وسائل تنازعات سے بچیں)
- Kover: `includedSourceSets = ["main", "functionalTest"]`, `check` پر HTML + XML رپورٹس

## بلڈ کمانڈز

```bash
./gradlew build                                   # مکمل بلڈ (UT + functionalTest + Cucumber)
./gradlew build -x test                            # صرف compile
./gradlew test                                     # JUnit5 یونٹ ٹیسٹس
./gradlew functionalTest                           # GradleTestKit فنکشنل
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # Playwright E2E (Chromium آٹو-انسٹال)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # کوریج ≥ 85 %
./gradlew publishToMavenLocal                      # مقامی اشاعت
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # E2E کے لیے Chromium انسٹال کریں
```

> **قاعدہ 0 (AGENT.adoc)**: کسی بھی سورس تبدیلی کے بعد `./gradlew -q publishToMavenLocal` **لازمی** ہے،
> کنزیومر پروجیکٹ سے کسی بھی فنکشنل ٹیسٹ سے پہلے۔

## CI پائپ لائن

`.github/workflows/test.yml` (ایک جاب، `ubuntu-latest`, ٹائم آؤٹ 20 min, JDK 24 temurin):
1. `./gradlew build` (UT + Cucumber) — `bakery-plugin/` میں
2. `./gradlew installPlaywright` — Chromium انسٹال کرتا ہے
3. `./gradlew e2eTest` — Playwright E2E

معاون ورک فلوز: `website.yml` (سائٹ تعیناتی), `readme_plantuml.yml` (readme-gradle کے ذریعے README جنریشن)۔

## اشاعت (NMCP)

`bakery-plugin/settings.gradle.kts` میں `com.gradleup.nmcp.settings` (`1.5.0`) کے ذریعے کنفیگر۔
اسنادیں `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`) سے پڑھی جاتی ہیں،
`publishingType = "AUTOMATIC"`۔
دستخط `useGpgCmd()` استعمال کرتے ہیں؛ `CI=true` یا ورژن `-SNAPSHOT` پر ختم ہو تو دستخط چھوڑ دیے جاتے ہیں۔
POM (`withType<MavenPublication>` پر) اعلام کرتا ہے:
- Apache 2.0 لائسنس، ڈویلپر `cccp-education` (cccp.edu@gmail.com)
- `github.com/cccp-education/bakery-gradle` کی طرف اشارہ کرتا SCM
- groupId مائیگریشن کے لیے اختیاری `relocationGroup` انجیکشن

`gradlePlugin` بلاک: `id = education.ccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName "Bakery Plugin", ٹیگز `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`۔

اشاعت کمانڈ:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## EPIC کی حیثیت

تمام EPIC بند (`bakery-plugin/.agents/INDEX.adoc` دیکھیں):
- EPICs 1–13 (گورننس، Supabase→Firebase، بلڈ، رابطہ فارم، Cucumber ٹائم آؤٹ، publishProfile،
  YAML رواداری، 0.1.4 ریلیز، سکارفولڈنگ TDD، ٹیسٹ مدت، KG، کوریج) — ✅
- **BKY-JB-0→9** (JBake ماہر: سکارفولڈ، Thymeleaf، Google Forms، Firebase Auth، Analytics،
  تھیم، لے آؤٹس، AI مضمون، E2E) — ✅
- **BKY-LENS-1→6** (LENtille: segregation, enrichment, budget, BDD rewrite, N3 connect, augmented templates) — ✅
- **BKY-IA-2** (پیرامیٹرک IA تھیم) — ✅
- **BKY-KOV-1** (Kover 78 % → 85.88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, session 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, session 143) — ✅
- **BKY-COV-2** (مائیکرو فیشر DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 زبانیں، 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (حقیقی مائیگریشن magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (قانونِ معاشیاتِ روشنائی، 3/3 pts) — ✅
- **BKY-CR-V5** (60 code smells) — ✅
- **BKY-CR-V6** (3 ساختی تکراریں، 8/8 pts) — ✅
- **BKY-A11Y-1** (WCAG/RGAA رسائی، 13/13 pts) — ✅
- **BKY-FIX-1** (site.yml کے بغیر deployProfile) — ✅

## فن تعمیر دستاویزات

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — مطلق قواعد اور گورننس
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — سیشنز، EPICs، روڈ میپ
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — باقی کام
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — موجودہ سیشن مشن
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — JBake ماہر روڈ میپ
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — کوڈ جائزہ V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — کوریج تجزیہ
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Lazy/Eager پیٹرن
- [README.adoc](../README.adoc) — پروجیکٹ-سطح README (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## تعاون

1. بلڈ compile ہوتا ہے: `./gradlew build -x test`
2. مقامی دوبارہ اشاعت: `./gradlew -q publishToMavenLocal` (قاعدہ 0, لازمي)
3. یونٹ ٹیسٹس سبز: `./gradlew test`
4. Cucumber سبز: `./gradlew cucumberTest` (129/129)
5. کوریج کا احترام: `./gradlew koverThresholdCheck` (≥ 85 %)
6. DDD کنونشنز پر عمل کریں (value objects, ports/adapters, Arrow `Either`/`Raise`, no leaks)
7. `site.yml` کو بیک اپ کے بغیر کبھی اووررائٹ نہ کریں (AGENT.adoc §1b — حفاظتی قاعدہ)
8. سیشن کے اختتام پر 6-قدمی اختتام کا طریقہ چلائیں (AGENT.adoc §3)

## لائسنس

Apache License 2.0 — [LICENCE](../bakery-plugin/LICENCE) دیکھیں۔

---

_CCCP Education ایکو سسٹم کا حصہ — `groupId: education.cccp`۔_
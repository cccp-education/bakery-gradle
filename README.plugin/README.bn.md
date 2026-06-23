<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — প্লাগইন অভ্যন্তরীণ

> `bakery-plugin` Gradle প্লাগইনের জন্য ডেভেলপার ও অবদানকারী গাইড।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **সংস্করণ**: `0.0.2` · **গ্রুপ**: `education.ccp` · **প্লাগইন ID**: `education.ccp.bakery`
- **টুলচেইন**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **বিল্ড**: `./gradlew build` · **পরীক্ষা**: `./gradlew check` · **কভারেজ গেট**: `./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## মডিউল বিন্যাস

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # প্লাগইন এন্ট্রি পয়েন্ট — সমস্ত টাস্ক নিবন্ধন করে (afterEvaluate ব্রাঞ্চিং)
│   │   ├── BakeryExtension.kt           # DSL এক্সটেনশন (configPath, siteType, language, ia, themes, a11y, …)
│   │   ├── BakeryConstants.kt           # টাস্ক গ্রুপ (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # site.yml মডেল (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (অ্যাগ্রিগেট)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # অ্যাক্সেসিবিলিটি WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille: segregation + enrichment + budget)
│   │   ├── article/ theme/ scaffold/   # ইন্টেন্ট DSLs (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # স্থানীয়ভাবে মিরর করা N0 চুক্তি (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # GradleTestKit ফাংশনাল পরীক্ষা
├── src/e2eTest/kotlin/bakery/e2e/       # Playwright E2E পরীক্ষা
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # ভার্সন ক্যাটালগ
```

## N0 চুক্তি (workspace-bom MEMPHIS থেকে)

| চুক্তি | আর্টিফ্যাক্ট | প্রদান করে |
|----------|----------|----------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(`contracts/i18n/`-এ মিরর)_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(`contracts/pipeline/`-এ মিরর)_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

BOM `bakery-plugin/build.gradle.kts:29`-এ `implementation(platform("education.cccp:workspace-bom:0.0.1"))` এর মাধ্যমে যুক্ত।

## মূল নির্ভরতা

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — স্ট্যাটিক সাইট ইঞ্জিন (api)
- **Thymeleaf** `3.0.14.RELEASE` — টেমপ্লেট রেন্ডারিং (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — gh-pages ডিপ্লয় (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — AsciiDoc ডায়াগ্রাম (api, jbake bundle-এ)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — BKY-IA-0 LLM সার্ভিস
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — OllamaLlmService-এর জন্য অ্যাসিঙ্ক
- **Arrow-kt** (core + fx-coroutines) — ফাংশনাল প্রোগ্রামিং (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — RSS পার্সিং (CS-8, ভঙ্গুর DocumentBuilderFactory DOM প্রতিস্থাপন করে)
- **commons-io** `2.13.0` — ফাইল ইউটিলিটি (api)
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — E2E ব্রাউজার পরীক্ষা
- **Jackson** (kotlin + yaml) — YAML কনফিগ, JSON সিরিয়ালাইজেশন
- **Graphify plugin** `0.0.2` — Knowledge Graph ইন্টিগ্রেশন (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — RAG / augmented context কনজিউমার

## Ollama ইনস্ট্যান্স (গ্লোবাল সীমাবদ্ধতা)

পোর্ট `11434–11436` নিষিদ্ধ। `11437–11465` (29 পোর্ট) এ রোটেট করুন।
অনুমোদিত মডেল: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`।
Bakery ডিফল্ট: `http://localhost:11464` + `gpt-oss:120b-cloud` (`IaConfig.kt` দেখুন)।

## পরীক্ষা ম্যাট্রিক্স

| টাস্ক | SourceSet | সুযোগ | টাইমআউট | প্যারালেলিজম |
|------|-----------|-------|---------|-------------|
| `test`           | test          | JUnit5 ইউনিট (Cucumber ইঞ্জিন + `bakery.scenarios.*` বাদ দেয়) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | GradleTestKit ফাংশনাল | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Playwright ব্রাউজার (`installPlaywright`-এ নির্ভরশীল) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | কভারেজ ≥ 85 % (Kover XML পার্স করে) | — | — |

ভেরিফিকেশন ওয়্যারিং (`build.gradle.kts`):
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` `check`-এ **যুক্ত নয়** — স্পষ্টভাবে `./gradlew e2eTest` দিয়ে চালান।
- Cucumber স্টেপ `src/test/scenarios`-এ, features `src/test/features`-এ।
- `testImplementation` `functionalTest` implementation থেকে প্রসারিত (বিপরীত নয়)।

## JVM টিউনিং

- সমস্ত `Test` টাস্ক: `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`: `maxHeapSize = "1g"`, `doLast`-এ 1h-এর পুরোনো স্টেল `gradle-test-*` টেম্প ডির পরিষ্কার করে
- `e2eTest`: `maxParallelForks = 1`, `forkEvery = 1` (ব্রাউজার রিসোর্স সংঘাত এড়ান)
- Kover: `includedSourceSets = ["main", "functionalTest"]`, `check`-এ HTML + XML রিপোর্ট

## বিল্ড কমান্ড

```bash
./gradlew build                                   # পূর্ণ বিল্ড (UT + functionalTest + Cucumber)
./gradlew build -x test                            # শুধু কমপাইল
./gradlew test                                     # JUnit5 ইউনিট পরীক্ষা
./gradlew functionalTest                           # GradleTestKit ফাংশনাল
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # Playwright E2E (Chromium অটো-ইনস্টল)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # কভারেজ ≥ 85 %
./gradlew publishToMavenLocal                      # স্থানীয় প্রকাশনা
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # E2E-এর জন্য Chromium ইনস্টল করুন
```

> **নিয়ম 0 (AGENT.adoc)**: যেকোনো সোর্স পরিবর্তনের পরে `./gradlew -q publishToMavenLocal` **বাধ্যতামূলক**,
> কনজিউমার প্রজেক্ট থেকে যেকোনো ফাংশনাল পরীক্ষার আগে।

## CI পাইপলাইন

`.github/workflows/test.yml` (একক কাজ, `ubuntu-latest`, টাইমআউট 20 min, JDK 24 temurin):
1. `./gradlew build` (UT + Cucumber) — `bakery-plugin/`-এ
2. `./gradlew installPlaywright` — Chromium ইনস্টল করে
3. `./gradlew e2eTest` — Playwright E2E

সহায়ক ওয়ার্কফ্লো: `website.yml` (সাইট ডিপ্লয়), `readme_plantuml.yml` (readme-gradle-এর মাধ্যমে README জেনারেশন)।

## প্রকাশনা (NMCP)

`bakery-plugin/settings.gradle.kts`-এ `com.gradleup.nmcp.settings` (`1.5.0`) এর মাধ্যমে কনফিগার।
ক্রেডেনশিয়াল `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`) থেকে পড়া হয়,
`publishingType = "AUTOMATIC"`।
সাইনিং `useGpgCmd()` ব্যবহার করে; `CI=true` বা ভার্সন `-SNAPSHOT` দিয়ে শেষ হলে সাইনিং বাদ দেওয়া হয়।
POM (`withType<MavenPublication>`-এ) ঘোষণা করে:
- Apache 2.0 লাইসেন্স, ডেভেলপার `cccp-education` (cccp.edu@gmail.com)
- `github.com/cccp-education/bakery-gradle`-এ নির্দেশ করা SCM
- groupId মাইগ্রেশনের জন্য ঐচ্ছিক `relocationGroup` ইনজেকশন

`gradlePlugin` ব্লক: `id = education.cccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName "Bakery Plugin", ট্যাগ `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`।

প্রকাশনা কমান্ড:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## EPIC অবস্থা

সমস্ত EPIC বন্ধ (`bakery-plugin/.agents/INDEX.adoc` দেখুন):
- EPICs 1–13 (গভর্ন্যান্স, Supabase→Firebase, বিল্ড, যোগাযোগ ফর্ম, Cucumber টাইমআউট, publishProfile,
  YAML টলারেন্স, 0.1.4 রিলিজ, স্ক্যাফোল্ডিং TDD, পরীক্ষার সময়কাল, KG, কভারেজ) — ✅
- **BKY-JB-0→9** (JBake এক্সপার্ট: স্ক্যাফোল্ড, Thymeleaf, Google Forms, Firebase Auth, Analytics,
  থিম, লেআউট, AI প্রবন্ধ, E2E) — ✅
- **BKY-LENS-1→6** (LENtille: segregation, enrichment, budget, BDD rewrite, N3 connect, augmented templates) — ✅
- **BKY-IA-2** (প্যারামেট্রিক IA থিম) — ✅
- **BKY-KOV-1** (Kover 78 % → 85.88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, সেশন 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, সেশন 143) — ✅
- **BKY-COV-2** (মাইক্রোফিশার DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 ভাষা, 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (বাস্তব মাইগ্রেশন magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (কালি অর্থনীতির সূত্র, 3/3 pts) — ✅
- **BKY-CR-V5** (60 code smells) — ✅
- **BKY-CR-V6** (3 টি কাঠামোগত পুনরাবৃত্তি, 8/8 pts) — ✅
- **BKY-A11Y-1** (WCAG/RGAA অ্যাক্সেসিবিলিটি, 13/13 pts) — ✅
- **BKY-FIX-1** (site.yml ছাড়া deployProfile) — ✅

## আর্কিটেকচার নথি

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — পরম নিয়ম ও গভর্ন্যান্স
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — সেশন, EPICs, রোডম্যাপ
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — অবশিষ্ট কাজ
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — বর্তমান সেশন মিশন
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — JBake এক্সপার্ট রোডম্যাপ
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — কোড রিভিউ V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — কভারেজ বিশ্লেষণ
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Lazy/Eager প্যাটার্ন
- [README.adoc](../README.adoc) — প্রজেক্ট-স্তরের README (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## অবদান

1. বিল্ড কমপাইল হয়: `./gradlew build -x test`
2. স্থানীয়ভাবে পুনঃপ্রকাশনা: `./gradlew -q publishToMavenLocal` (নিয়ম 0, বাধ্যতামূলক)
3. ইউনিট পরীক্ষা সবুজ: `./gradlew test`
4. Cucumber সবুজ: `./gradlew cucumberTest` (129/129)
5. কভারেজ সম্মান: `./gradlew koverThresholdCheck` (≥ 85 %)
6. DDD রীতিনীতি অনুসরণ করুন (value objects, ports/adapters, Arrow `Either`/`Raise`, no leaks)
7. ব্যাকআপ ছাড়া `site.yml` অধিলেখিত করবেন না (AGENT.adoc §1b — সুরক্ষা নিয়ম)
8. সেশন শেষে 6-ধাপ সমাপনী প্রক্রিয়া চালান (AGENT.adoc §3)

## লাইসেন্স

Apache License 2.0 — [LICENCE](../bakery-plugin/LICENCE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_
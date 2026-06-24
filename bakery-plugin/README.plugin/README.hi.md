<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — प्लगइन आंतरिक गाइड

> `bakery-plugin` Gradle प्लगइन के लिए डेवलपर और योगदानकर्ता गाइड।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **संस्करण**: `0.0.2` · **Group**: `education.ccp` · **प्लगइन ID**: `education.ccp.bakery`
- **टूलचेन**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **बिल्ड**: `./gradlew build` · **परीक्षण**: `./gradlew check` · **कवरेज गेट**: `./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## मॉड्यूल लेआउट

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # प्लगइन प्रवेश बिंदु — सभी कार्य पंजीकृत करता है (afterEvaluate ब्रांचिंग)
│   │   ├── BakeryExtension.kt           # DSL एक्सटेंशन (configPath, siteType, language, ia, themes, a11y, …)
│   │   ├── BakeryConstants.kt           # कार्य समूह (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # site.yml मॉडल (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (समुच्चय)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # पहुँच WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille: segregation + enrichment + budget)
│   │   ├── article/ theme/ scaffold/   # इरादा DSLs (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # स्थानीय रूप से मिरर किए गए N0 अनुबंध (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # GradleTestKit कार्यात्मक परीक्षण
├── src/e2eTest/kotlin/bakery/e2e/       # Playwright E2E परीक्षण
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # संस्करण कैटलॉग
```

## N0 अनुबंध (workspace-bom MEMPHIS से)

| अनुबंध | कलाकृति | प्रदान करता है |
|----------|----------|----------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(`contracts/i18n/` में मिरर)_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(`contracts/pipeline/` में मिरर)_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

BOM `bakery-plugin/build.gradle.kts:29` में `implementation(platform("education.cccp:workspace-bom:0.0.1"))` के माध्यम से जुड़ा है।

## प्रमुख निर्भरताएँ

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — स्थैतिक साइट इंजन (api)
- **Thymeleaf** `3.0.14.RELEASE` — टेम्पलेट रेंडरिंग (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — gh-pages तैनाती (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — AsciiDoc आरेख (api, jbake bundle में)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — BKY-IA-0 LLM सेवा
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — OllamaLlmService के लिए अतुल्यकालिक
- **Arrow-kt** (core + fx-coroutines) — फंक्शनल प्रोग्रामिंग (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — RSS पार्सिंग (CS-8, भंगुर DocumentBuilderFactory DOM को प्रतिस्थापित करता है)
- **commons-io** `2.13.0` — फ़ाइल उपयोगिताएँ (api)
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — E2E ब्राउज़र परीक्षण
- **Jackson** (kotlin + yaml) — YAML कॉन्फ़िग, JSON क्रमानुवादन
- **Graphify plugin** `0.0.2` — नॉलेज ग्राफ एकीकरण (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — RAG / संवर्धित संदर्भ उपभोक्ता

## Ollama इंस्टेंस (वैश्विक बाधा)

पोर्ट `11434–11436` निषिद्ध हैं। `11437–11465` (29 पोर्ट) पर रोटेट करें।
अधिकृत मॉडल: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`।
Bakery डिफ़ॉल्ट: `http://localhost:11464` + `gpt-oss:120b-cloud` (`IaConfig.kt` देखें)।

## परीक्षण मैट्रिक्स

| कार्य | SourceSet | दायरा | समय समाप्ति | समानांतरता |
|------|-----------|-------|---------|-------------|
| `test`           | test          | JUnit5 यूनिट (Cucumber इंजन + `bakery.scenarios.*` को बाहर रखता है) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | GradleTestKit कार्यात्मक | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Playwright ब्राउज़र (`installPlaywright` पर निर्भर) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | कवरेज ≥ 85 % (Kover XML पार्स करता है) | — | — |

सत्यापन वायरिंग (`build.gradle.kts`):
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` `check` में **नहीं** जुड़ा है — स्पष्ट रूप से `./gradlew e2eTest` से चलाएँ।
- Cucumber चरण `src/test/scenarios` में, features `src/test/features` में।
- `testImplementation` `functionalTest` implementation से विस्तारित होता है (विपरीत नहीं)।

## JVM ट्यूनिंग

- सभी `Test` कार्य: `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`: `maxHeapSize = "1g"`, `doLast` में 1h से पुराने स्थिर `gradle-test-*` टेम्प डायर साफ़ करता है
- `e2eTest`: `maxParallelForks = 1`, `forkEvery = 1` (ब्राउज़र संसाधन संघर्ष से बचें)
- Kover: `includedSourceSets = ["main", "functionalTest"]`, `check` पर HTML + XML रिपोर्ट

## बिल्ड कमांड

```bash
./gradlew build                                   # पूर्ण बिल्ड (UT + functionalTest + Cucumber)
./gradlew build -x test                            # केवल कंपाइल
./gradlew test                                     # JUnit5 यूनिट परीक्षण
./gradlew functionalTest                           # GradleTestKit कार्यात्मक
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # Playwright E2E (Chromium ऑटो-इंस्टॉल)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # कवरेज ≥ 85 %
./gradlew publishToMavenLocal                      # स्थानीय प्रकाशन
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # E2E के लिए Chromium इंस्टॉल करें
```

> **नियम 0 (AGENT.adoc)**: किसी भी स्रोत परिवर्तन के बाद `./gradlew -q publishToMavenLocal` **अनिवार्य** है,
> उपभोक्ता प्रोजेक्ट से किसी भी फंक्शनल परीक्षण से पहले।

## CI पाइपलाइन

`.github/workflows/test.yml` (एकल कार्य, `ubuntu-latest`, समय समाप्ति 20 min, JDK 24 temurin):
1. `./gradlew build` (UT + Cucumber) — `bakery-plugin/` में
2. `./gradlew installPlaywright` — Chromium इंस्टॉल करता है
3. `./gradlew e2eTest` — Playwright E2E

सहायक वर्कफ़्लो: `website.yml` (साइट तैनाती), `readme_plantuml.yml` (readme-gradle द्वारा README निर्माण)।

## प्रकाशन (NMCP)

`bakery-plugin/settings.gradle.kts` में `com.gradleup.nmcp.settings` (`1.5.0`) के माध्यम से कॉन्फ़िगर किया गया।
क्रेडेंशियल `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`) से पढ़े जाते हैं,
`publishingType = "AUTOMATIC"`।
साइनिंग `useGpgCmd()` का उपयोग करती है; जब `CI=true` या संस्करण `-SNAPSHOT` पर समाप्त होता है तो साइनिंग छोड़ दी जाती है।
POM (`withType<MavenPublication>` पर) घोषित करता है:
- Apache 2.0 लाइसेंस, डेवलपर `cccp-education` (cccp.edu@gmail.com)
- `github.com/cccp-education/bakery-gradle` की ओर इशारा करता SCM
- groupId माइग्रेशन के लिए वैकल्पिक `relocationGroup` इंजेक्शन

`gradlePlugin` ब्लॉक: `id = education.cccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName "Bakery Plugin", टैग `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`।

प्रकाशन कमांड:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## EPIC स्थिति

सभी EPIC बंद (देखें `bakery-plugin/.agents/INDEX.adoc`):
- EPICs 1–13 (शासन, Supabase→Firebase, बिल्ड, संपर्क फ़ॉर्म, Cucumber समय समाप्ति, publishProfile,
  YAML सहनशीलता, 0.1.4 रिलीज़, स्कैफ़ोल्डिंग TDD, परीक्षण अवधि, KG, कवरेज) — ✅
- **BKY-JB-0→9** (JBake विशेषज्ञ: स्कैफ़ोल्ड, Thymeleaf, Google Forms, Firebase Auth, Analytics,
  थीम, लेआउट, AI लेख, E2E) — ✅
- **BKY-LENS-1→6** (LENtille: segregation, enrichment, budget, BDD rewrite, N3 connect, augmented templates) — ✅
- **BKY-IA-2** (पैरामीट्रिक IA थीम) — ✅
- **BKY-KOV-1** (Kover 78 % → 85.88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, session 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, session 143) — ✅
- **BKY-COV-2** (माइक्रोफिशर DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 भाषाएँ, 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (वास्तविक माइग्रेशन magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (स्याही अर्थव्यवस्था का नियम, 3/3 pts) — ✅
- **BKY-CR-V5** (60 कोड स्मेल) — ✅
- **BKY-CR-V6** (3 संरचनात्मक दोहराव, 8/8 pts) — ✅
- **BKY-A11Y-1** (WCAG/RGAA पहुँच, 13/13 pts) — ✅
- **BKY-FIX-1** (site.yml के बिना deployProfile) — ✅

## आर्किटेक्चर दस्तावेज़

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — निरपेक्ष नियम और शासन
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — सत्र, EPICs, रोडमैप
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — शेष कार्य
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — वर्तमान सत्र मिशन
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — JBake विशेषज्ञ रोडमैप
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — कोड समीक्षा V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — कवरेज विश्लेषण
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Lazy/Eager पैटर्न
- [README.adoc](../README.adoc) — प्रोजेक्ट-स्तर README (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## योगदान

1. बिल्ड कंपाइल होता है: `./gradlew build -x test`
2. स्थानीय रूप से पुनः प्रकाशित करें: `./gradlew -q publishToMavenLocal` (नियम 0, अनिवार्य)
3. यूनिट परीक्षण हरे: `./gradlew test`
4. Cucumber हरे: `./gradlew cucumberTest` (129/129)
5. कवरेज का पालन: `./gradlew koverThresholdCheck` (≥ 85 %)
6. DDD सम्मेलनों का पालन करें (value objects, ports/adapters, Arrow `Either`/`Raise`, no leaks)
7. `site.yml` को बिना बैकअप के कभी अधिलेखित न करें (AGENT.adoc §1b — सुरक्षा नियम)
8. सत्र अंत में 6-चरण समापन प्रक्रिया चलाएँ (AGENT.adoc §3)

## लाइसेंस

Apache License 2.0 — [LICENCE](../bakery-plugin/LICENCE) देखें।

---

_CCCP Education इकोसिस्टम का हिस्सा — `groupId: education.cccp`।_
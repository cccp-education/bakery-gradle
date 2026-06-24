<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — داخليات الإضافة

> دليل المطورين والمساهمين لإضافة Gradle `bakery-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **الإصدار**: `0.0.2` · **المجموعة**: `education.ccp` · **معرف الإضافة**: `education.ccp.bakery`
- **سلسلة الأدوات**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **البناء**: `./gradlew build` · **الاختبارات**: `./gradlew check` · **بوابة التغطية**: `./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## تخطيط الوحدات

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # نقطة دخول الإضافة — تسجل جميع المهام (تفريع afterEvaluate)
│   │   ├── BakeryExtension.kt           # امتداد DSL (configPath, siteType, language, ia, themes, a11y, …)
│   │   ├── BakeryConstants.kt           # مجموعات المهام (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # نموذج site.yml (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (مجمّع)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # إمكانية الوصول WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille: segregation + enrichment + budget)
│   │   ├── article/ theme/ scaffold/   # DSLs النية (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # عقود N0 منعكسة محلياً (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # اختبارات وظيفية GradleTestKit
├── src/e2eTest/kotlin/bakery/e2e/       # اختبارات E2E Playwright
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # كتالوج الإصدارات
```

## عقود N0 (من workspace-bom MEMPHIS)

| العقد | القطعة | توفر |
|----------|----------|----------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(منعكس في `contracts/i18n/`)_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(منعكس في `contracts/pipeline/`)_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

يتم ربط BOM عبر `implementation(platform("education.cccp:workspace-bom:0.0.1"))` في
`bakery-plugin/build.gradle.kts:29`.

## التبعيات الرئيسية

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — محرك موقع ثابت (api)
- **Thymeleaf** `3.0.14.RELEASE` — عرض القوالب (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — نشر gh-pages (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — مخططات AsciiDoc (api, في حزمة jbake)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — خدمة LLM لـ BKY-IA-0
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — غير متزامن لـ OllamaLlmService
- **Arrow-kt** (core + fx-coroutines) — البرمجة الوظيفية (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — تحليل RSS (CS-8، يستبدل DocumentBuilderFactory DOM الهش)
- **commons-io** `2.13.0` — أدوات الملفات (api)
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — اختبارات المتصفح E2E
- **Jackson** (kotlin + yaml) — إعداد YAML، تسلسل JSON
- **Graphify plugin** `0.0.2` — تكامل Knowledge Graph (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — مستهلك RAG / السياق المعزز

## مثيلات Ollama (قيد عام)

المنافذ `11434–11436` ممنوعة. التبديل عبر `11437–11465` (29 منفذ).
النماذج المعتمدة: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
افتراضي Bakery: `http://localhost:11464` + `gpt-oss:120b-cloud` (راجع `IaConfig.kt`).

## مصفوفة الاختبارات

| المهمة | SourceSet | النطاق | المهلة | التوازي |
|------|-----------|-------|---------|-------------|
| `test`           | test          | وحدة JUnit5 (يستثني محرك Cucumber + `bakery.scenarios.*`) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | وظيفي GradleTestKit | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | متصفح Playwright (يعتمد على `installPlaywright`) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | تغطية ≥ 85 % (يحلل XML لـ Kover) | — | — |

توصيل التحقق (`build.gradle.kts`):
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` **غير** موصول بـ `check` — شغّله صراحةً عبر `./gradlew e2eTest`.
- خطوات Cucumber في `src/test/scenarios`، features في `src/test/features`.
- `testImplementation` يمتد من implementation الخاص بـ `functionalTest` (وليس العكس).

## ضبط JVM

- جميع مهام `Test`: `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`: `maxHeapSize = "1g"`, ينظف مجلدات `gradle-test-*` المؤقتة المتقادمة لأكثر من ساعة في `doLast`
- `e2eTest`: `maxParallelForks = 1`, `forkEvery = 1` (تجنب تعارض موارد المتصفح)
- Kover: `includedSourceSets = ["main", "functionalTest"]`, تقارير HTML + XML عند `check`

## أوامر البناء

```bash
./gradlew build                                   # بناء كامل (UT + functionalTest + Cucumber)
./gradlew build -x test                            # compile فقط
./gradlew test                                     # اختبارات وحدة JUnit5
./gradlew functionalTest                           # وظيفي GradleTestKit
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # Playwright E2E (تثبيت تلقائي لـ Chromium)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # تغطية ≥ 85 %
./gradlew publishToMavenLocal                      # نشر محلي
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # تثبيت Chromium لـ E2E
```

> **القاعدة 0 (AGENT.adoc)**: `./gradlew -q publishToMavenLocal` **إلزامي** بعد أي تغيير في المصدر،
> قبل أي اختبار وظيفي من مشروع مستهلك.

## خط أنابيب CI

`.github/workflows/test.yml` (وظيفة واحدة، `ubuntu-latest`, مهلة 20 min, JDK 24 temurin):
1. `./gradlew build` (UT + Cucumber) — في `bakery-plugin/`
2. `./gradlew installPlaywright` — يثبّت Chromium
3. `./gradlew e2eTest` — Playwright E2E

تدفقات العمل المساعدة: `website.yml` (نشر الموقع), `readme_plantuml.yml` (توليد README عبر readme-gradle).

## النشر (NMCP)

مُهيأ عبر `com.gradleup.nmcp.settings` (`1.5.0`) في `bakery-plugin/settings.gradle.kts`.
تُقرأ بيانات الاعتماد من `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`)،
`publishingType = "AUTOMATIC"`.
يستخدم التوقيع `useGpgCmd()`؛ يُتخطى التوقيع عند `CI=true` أو انتهاء الإصدار بـ `-SNAPSHOT`.
يعلن POM (على `withType<MavenPublication>`):
- ترخيص Apache 2.0، المطور `cccp-education` (cccp.edu@gmail.com)
- SCM يشير إلى `github.com/cccp-education/bakery-gradle`
- حقن اختياري لـ `relocationGroup` لترحيل groupId

كتلة `gradlePlugin`: `id = education.cccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName "Bakery Plugin", الوسوم `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`.

أمر النشر:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## حالة EPICs

جميع EPICs مغلقة (راجع `bakery-plugin/.agents/INDEX.adoc`):
- EPICs 1–13 (الحوكمة، Supabase→Firebase، البناء، نموذج الاتصال، مهلة Cucumber، publishProfile،
  تسامح YAML، إصدار 0.1.4، السقالة TDD، مدة الاختبارات، KG، التغطية) — ✅
- **BKY-JB-0→9** (خبير JBake: سقالة، Thymeleaf، Google Forms، Firebase Auth، Analytics،
  سمة، تخطيطات، مقالة IA، E2E) — ✅
- **BKY-LENS-1→6** (LENtille: segregation, enrichment, budget, إعادة كتابة BDD, اتصال N3, قوالب معززة) — ✅
- **BKY-IA-2** (سمة IA بارامترية) — ✅
- **BKY-KOV-1** (Kover 78 % → 85.88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, جلسة 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, جلسة 143) — ✅
- **BKY-COV-2** (شقوق دقيقة DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 لغات, 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (ترحيل حقيقي magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (قانون اقتصاد الحبر, 3/3 pts) — ✅
- **BKY-CR-V5** (60 code smells) — ✅
- **BKY-CR-V6** (3 تكرارات هيكلية, 8/8 pts) — ✅
- **BKY-A11Y-1** (إمكانية وصول WCAG/RGAA, 13/13 pts) — ✅
- **BKY-FIX-1** (deployProfile بدون site.yml) — ✅

## وثائق البنية

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — قواعد مطلقة وحوكمة
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — الجلسات، EPICs, خارطة الطريق
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — العمل المتبقي
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — مهمة الجلسة الحالية
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — خارطة طريق خبير JBake
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — مراجعة الكود V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — تحليل التغطية
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — نمط Lazy/Eager
- [README.adoc](../README.adoc) — README على مستوى المشروع (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## المساهمة

1. البناء يُترجم: `./gradlew build -x test`
2. إعادة النشر محلياً: `./gradlew -q publishToMavenLocal` (القاعدة 0، إلزامي)
3. اختبارات الوحدة خضراء: `./gradlew test`
4. Cucumber أخضر: `./gradlew cucumberTest` (129/129)
5. احترام التغطية: `./gradlew koverThresholdCheck` (≥ 85 %)
6. اتباع اصطلاحات DDD (value objects, ports/adapters, Arrow `Either`/`Raise`, بدون تسريبات)
7. عدم الكتابة فوق `site.yml` بدون نسخة احتياطية (AGENT.adoc §1b — قاعدة سلامة)
8. تشغيل إجراء الإغلاق ذو 6 خطوات (AGENT.adoc §3) في نهاية الجلسة

## الترخيص

Apache License 2.0 — راجع [LICENCE](../bakery-plugin/LICENCE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`._
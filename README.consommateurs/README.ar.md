<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — دليل المستهلك

> إضافة Gradle لتوليد المواقع الثابتة — JBake + Thymeleaf + Firebase Auth + Analytics + Knowledge Graph + LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **الإصدار**: `0.0.2` · **المجموعة**: `education.ccp` · **معرف الإضافة**: `education.ccp.bakery`
- **البناء**: `./gradlew build` · **الاختبارات**: `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **التغطية**: ≥ 85 % (Kover `koverThresholdCheck`، متصل بـ `check`) · **Cucumber**: 129/129 PASS

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## ماذا يفعل

يقوم `bakery-gradle` بتوليد ونشر موقع ثابت عبر **JBake 2.7.0** (قوالب Thymeleaf، محتوى AsciiDoc/Markdown). يدمج Firebase Auth + التعليقات، Google Forms، Analytics/Newsletter، نظام سمات، تدقيق إمكانية الوصول (WCAG/RGAA)، ترحيل i18n إلى 10 لغات، توليد مقالات/سقالة/سمة بمساعدة الذكاء الاصطناعي (LangChain4j + Ollama)، ونمط السياق المعزز **LENtille** الذي يغذي السياق المركب N3 لـ runner-gradle. مهمة `publishSite` المجمعة تخبز وتنشر إلى `gh-pages` في أمر واحد.

جزء من منظومة CCCP Education متعددة الإضافات:

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                 ↑
                     codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## البداية السريعة

### 1. تطبيق الإضافة

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. التهيئة عبر DSL أو `site.yml`

```gradle
bakery {
    configPath = file("site.yml").absolutePath
    siteType    = "blog"        // "blog" | "basic"
    language    = "fr"          // ISO 639-1
    supportedLanguages = listOf("fr", "en", "ar")
    ia {
        baseUrl  = "http://localhost:11464"
        modelName = "gpt-oss:120b-cloud"
    }
}
```

`site.yml` (غير متتبع في الإصدارات — لا تكتب فوقه أبداً دون نسخة احتياطية) يتحكم في JBake والنشر وFirebase وAnalytics والسمات والتخطيط وتهيئة السياق المعزز.

### 3. الخبز والنشر

```bash
./gradlew publishSite        # مجمّع: bake + نشر إلى gh-pages
./gradlew bake               # عرض JBake فقط
./gradlew deploySite          # دفع المخرجات المخبوزة إلى gh-pages
```

## المهام المتاحة

| المهمة | المجموعة | الوصف |
|------|-------|-------------|
| `bake`                      | —        | عرض JBake (مهيأ بواسطة `configureBakeTask`) |
| `generateSite`              | generate | تهيئة مجلدات الموقع + maquette (النوع: `blog`/`basic`) |
| `generateSiteFromIntention` | generate | سقالة موقع بمساعدة الذكاء الاصطناعي (تفاعلي، Ollama) |
| `generateTheme`             | generate | توليد سمة من الكتالوج (متغير + تجاوزات) |
| `generateArticle`           | generate | مقالة مدونة بمساعدة الذكاء الاصطناعي → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | ترحيل موقع bakery موجود إلى i18n (مسح القوالب، استخراج النصوص الثابتة، توليد `messages_{lang}.properties`) |
| `pagefind`                  | transform | فهرسة الموقع المخبوز باستخدام Pagefind للبحث في النص الكامل |
| `deploySite`                | deploy    | دفع المخرجات المخبوزة إلى فرع `gh-pages` على GitHub (JGit) |
| `deployMaquette`            | deploy    | دفع ملفات maquette إلى المستودع |
| `deployProfile`             | deploy    | دفع ملفات الملف الشخصي (مثل README.md) إلى مستودع GitHub |
| `publishSite`               | publish   | **مجمّع** — bake + نشر `gh-pages` (للراحة) |
| `collectSiteConfig`         | collect   | تهيئة إعدادات Bakery |
| `collectSiteContext`        | collect   | جمع سياق الموقع المخبوز → `build/bakery/metadata.json` لـ runner-gradle N3 |
| `collectAugmentedContext`   | collect   | جمع السياق المعزز LENS (segregation + enrichment + budget) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | خدمة الموقع المخبوز محلياً (JavaExec) |
| `validateFirebaseConfig`   | validate  | التحقق من اتساق تهيئة Firebase (ميكانيكي + IA اختياري) |
| `verifyConfigurationMapping`| verification | التحقق من تعيين YAML `site.yml` → `SiteConfiguration` وإخفاء الأسرار |
| `accessibilityAudit`       | audit     | تدقيق إمكانية الوصول WCAG/RGAA للموقع المخبوز — تقرير JSON/ASCII |
| `installPlaywright`         | verification | تثبيت متصفح Playwright Chromium لاختبارات E2E |

> **وضع التدهور**: عند غياب/عدم صلاحية `configPath`، تتراجع الإضافة إلى وضع **السقالة فقط** (`generateSite`, `generateSiteFromIntention`, `deployProfile`). اضبط `configPath` عبر DSL أو `gradle.properties` (`bakery.config.path=site.yml`) أو `-Pbakery.config.path=...`.

## امتداد DSL

```gradle
bakery {
    configPath        = "site.yml"
    sitesBaseDir      = "..."
    siteName          = "..."
    siteType          = "blog"          // "blog" | "basic"
    language          = "fr"
    supportedLanguages = listOf("fr", "en", "zh", "hi", "es", "ar", "bn", "pt", "ru", "ur")

    ia {
        baseUrl   = "http://localhost:11464"
        modelName = "gpt-oss:120b-cloud"
    }
    googleForms   { /* BKY-JB-3 */ }
    firebaseAuth  { /* BKY-JB-4 */ }
    commentsConfig { /* BKY-JB-4 Firestore */ }
    analytics     { /* BKY-JB-5 Plausible/Matomo */ }
    newsletter    { /* BKY-JB-5 mail footer */ }
    theme         { /* BKY-JB-6 CSS vars, dark/light, logo */ }
    layout        { /* BKY-JB-7 FULL_WIDTH, SIDEBAR_LEFT, SIDEBAR_RIGHT, CENTERED */ }
    articleIntention  { /* BKY-JB-8 topic, ton, audience, keywords, lang */ }
    augmentedContext  { /* BKY-LENS budget + injection */ }
    scaffoldIntention { /* BKY-IA-1 description, siteType, lang, projectName */ }
    themeIntention    { /* BKY-IA-2 description, variante, surcharges */ }
    i18nMigration     { /* BKY-I18N-MIG siteDir, languages, defaultLanguage, dryRun */ }
    a11y              { /* BKY-A11Y-1 auditDir, reportPath, conformanceLevel (AA/AAA), failOnNonCompliant */ }
}
```

## المتطلبات الأساسية

- **Java** 24+ (سلسلة أدوات Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **Node.js** (لـ Pagefind + Playwright عبر إضافة Gradle Node `7.1.0`)
- **Ollama** على المنفذ `11437–11465` (المنافذ `11434–11436` ممنوعة) — النماذج `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (فقط للنشر إلى Maven Central)

## البناء والاختبار

```bash
./gradlew build                     # بناء كامل (UT + functionalTest + Cucumber)
./gradlew test                      # اختبارات وحدة JUnit5 (تستثني محرك Cucumber)
./gradlew functionalTest            # اختبارات وظيفية GradleTestKit
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # Playwright E2E (يثبّت Chromium)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # تغطية ≥ 85 %
./gradlew publishToMavenLocal       # نشر محلي
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## استكشاف الأخطاء وإصلاحها

| العَرَض | الإصلاح |
|---------|-----|
| `configPath is not set`          | اضبط `bakery { configPath = "site.yml" }` أو `gradle.properties` أو `-Pbakery.config.path=...` |
| `Failed to read site.yml`        | تحقق من وجود الملف وأنه YAML صالح؛ تتراجع الإضافة إلى السقالة فقط |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Playwright Chromium مفقود      | `./gradlew installPlaywright` (يشغّلها `e2eTest` تلقائياً) |
| رفض اتصال Ollama        | تحقق من المنفذ في `11437–11465`؛ `11434–11436` ممنوعة |
| رفض الدفع إلى gh-pages             | تحقق من مفتاح SSH لـ JGit + تهيئة pushPage في `site.yml` |

راجع [AGENT.adoc](../bakery-plugin/AGENT.adoc) للحوكمة و[README.adoc](../README.adoc) لسياق المشروع.

## الترخيص

Apache License 2.0 — راجع [LICENSE](../bakery-plugin/LICENCE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`._
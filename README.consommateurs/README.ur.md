<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — صارفین کی گائیڈ

> جامد سائٹ جنریشن کے لیے Gradle پلگ ان — JBake + Thymeleaf + Firebase Auth + Analytics + Knowledge Graph + LLM۔

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **ورژن**: `0.0.2` · **گروپ**: `education.ccp` · **پلگ ان ID**: `education.ccp.bakery`
- **بلڈ**: `./gradlew build` · **ٹیسٹ**: `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **کوریج**: ≥ 85 % (Kover `koverThresholdCheck`، `check` میں جڑا ہوا) · **Cucumber**: 129/129 PASS

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## یہ کیا کرتا ہے

`bakery-gradle` **JBake 2.7.0** (Thymeleaf ٹیمپلیٹس، AsciiDoc/Markdown مواد) کے ذریعے ایک جامد سائٹ جنریٹ اور تعینات کرتا ہے۔ یہ Firebase Auth + تبصرے، Google Forms، Analytics/Newsletter، ایک تھیمنگ سسٹم، رسائی آڈٹ (WCAG/RGAA)، 10 زبانوں میں i18n مائیگریشن، AI-معاون مضمون/سکارفولڈ/تھیم جنریشن (LangChain4j + Ollama)، اور **LENtille** اugmented-context پیٹرن ایمبیڈ کرتا ہے جو runner-gradle کے N3 کمپوزٹ کنٹیکسٹ کو کھلاتا ہے۔ `publishSite` ایگریگیٹ ٹاسک ایک ہی کمانڈ میں بیک کرتا ہے اور `gh-pages` پر تعینات کرتا ہے۔

CCCP Education ملٹی پلگ ان ایکو سسٹم کا حصہ:

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                 ↑
                     codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## فوری آغاز

### 1. پلگ ان لگائیں

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. DSL یا `site.yml` کے ذریعے کنفیگر کریں

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

`site.yml` (ورژن کنٹرول میں نہیں — بیک اپ کے بغیر کبھی اووررائٹ نہ کریں) JBake، تعیناتی، Firebase، Analytics، تھیم، لے آؤٹ، اور augmented-context کنفیگریشن کو کنٹرول کرتا ہے۔

### 3. بیک اور تعینات کریں

```bash
./gradlew publishSite        # ایگریگیٹ: bake + gh-pages پر تعیناتی
./gradlew bake               # صرف JBake رینڈر
./gradlew deploySite          # بیک شدہ آؤٹ پٹ کو gh-pages پر دھکیلیں
```

## دستیاب ٹاسکس

| ٹاسک | گروپ | تفصیل |
|------|-------|-------------|
| `bake`                      | —        | JBake رینڈر (`configureBakeTask` کے ذریعے کنفیگر کردہ) |
| `generateSite`              | generate | سائٹ + maquette فولڈرز شروع کریں (قسم: `blog`/`basic`) |
| `generateSiteFromIntention` | generate | AI-معاون سائٹ سکارفولڈ (انٹرایکٹو، Ollama) |
| `generateTheme`             | generate | کیٹلاگ سے تھیم جنریٹ کریں (ویریئنٹ + اووررائڈز) |
| `generateArticle`           | generate | AI-معاون بلاگ مضمون → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | موجودہ bakery سائٹ کو i18n میں مائیگریٹ کریں (ٹیمپلیٹس اسکین کریں، ہارڈکوڈڈ ٹیکسٹ نکالیں، `messages_{lang}.properties` جنریٹ کریں) |
| `pagefind`                  | transform | بیک شدہ سائٹ کو Pagefind کے ساتھ مکمل متن تلاش کے لیے انڈیکس کریں |
| `deploySite`                | deploy    | بیک شدہ آؤٹ پٹ کو GitHub `gh-pages` برانچ پر دھکیلیں (JGit) |
| `deployMaquette`            | deploy    | maquette فائلوں کو ریپوزیٹری میں دھکیلیں |
| `deployProfile`             | deploy    | پروفائل فائلوں (جیسے README.md) کو GitHub ریپوزیٹری میں دھکیلیں |
| `publishSite`               | publish   | **ایگریگیٹ** — bake + `gh-pages` تعیناتی (سہولت) |
| `collectSiteConfig`         | collect   | Bakery کنفیگریشن شروع کریں |
| `collectSiteContext`        | collect   | بیک شدہ سائٹ کنٹیکسٹ جمع کریں → `build/bakery/metadata.json` (runner-gradle N3 کے لیے) |
| `collectAugmentedContext`   | collect   | LENS augmented کنٹیکسٹ جمع کریں (segregation + enrichment + budget) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | بیک شدہ سائٹ کو مقامی طور پر پیش کریں (JavaExec) |
| `validateFirebaseConfig`   | validate  | Firebase کنفیگریشن مستقل مزاجی کی تصدیق کریں (میکانیکی + اختیاری IA) |
| `verifyConfigurationMapping`| verification | YAML `site.yml` → `SiteConfiguration` میپنگ کی تصدیق کریں اور رازوں کو چھپائیں |
| `accessibilityAudit`       | audit     | بیک شدہ سائٹ کا WCAG/RGAA رسائی آڈٹ — JSON/ASCII رپورٹ |
| `installPlaywright`         | verification | E2E ٹیسٹس کے لیے Playwright Chromium براؤزر انسٹال کریں |

> **ڈیگریڈیشن موڈ**: جب `configPath` غائب/غیر valide ہو، پلگ ان **صرف-سکارفولڈ** موڈ میں گر جاتا ہے (`generateSite`, `generateSiteFromIntention`, `deployProfile`)۔ DSL، `gradle.properties` (`bakery.config.path=site.yml`)، یا `-Pbakery.config.path=...` کے ذریعے `configPath` سیٹ کریں۔

## ایکسٹینشن DSL

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

## ضروریات

- **Java** 24+ (Kotlin 2.3.20 ٹول چین)
- **Gradle** 9.5.1+
- **Node.js** (Gradle Node پلگ ان `7.1.0` کے ذریعے Pagefind + Playwright کے لیے)
- **Ollama** پورٹ `11437–11465` پر (پورٹس `11434–11436` ممنوعہ) — ماڈلز `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (صرف Maven Central پر اشاعت کے لیے)

## بلڈ اور ٹیسٹ

```bash
./gradlew build                     # مکمل بلڈ (UT + functionalTest + Cucumber)
./gradlew test                      # JUnit5 یونٹ ٹیسٹس (Cucumber انجن کو خارج کرتا ہے)
./gradlew functionalTest            # GradleTestKit فنکشنل ٹیسٹس
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # Playwright E2E (Chromium انسٹال کرتا ہے)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # کوریج ≥ 85 %
./gradlew publishToMavenLocal       # مقامی اشاعت
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## مسائل کا حل

| علامت | حل |
|---------|-----|
| `configPath is not set`          | `bakery { configPath = "site.yml" }`, `gradle.properties`, یا `-Pbakery.config.path=...` سیٹ کریں |
| `Failed to read site.yml`        | چیک کریں کہ فائل موجود ہے اور valide YAML ہے؛ پلگ ان صرف-سکارفولڈ پر گر جاتا ہے |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Playwright Chromium غائب      | `./gradlew installPlaywright` (`e2eTest` کے ذریعے خودکار) |
| Ollama کنکشن مسترد        | پورٹ `11437–11465` میں تصدیق کریں؛ `11434–11436` ممنوعہ ہیں |
| gh-pages پش مسترد             | JGit SSH کلید + `site.yml` pushPage کنفیگ چیک کریں |

گورننس کے لیے [AGENT.adoc](../bakery-plugin/AGENT.adoc) اور پروجیکٹ کنٹیکسٹ کے لیے [README.adoc](../README.adoc) دیکھیں۔

## لائسنس

Apache License 2.0 — [LICENSE](../bakery-plugin/LICENCE) دیکھیں۔

---

_CCCP Education ایکو سسٹم کا حصہ — `groupId: education.cccp`۔_
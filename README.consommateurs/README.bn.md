<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — ভোক্তা গাইড

> স্ট্যাটিক সাইট জেনারেশনের জন্য Gradle প্লাগইন — JBake + Thymeleaf + Firebase Auth + Analytics + Knowledge Graph + LLM।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **সংস্করণ**: `0.0.2` · **গ্রুপ**: `education.ccp` · **প্লাগইন ID**: `education.ccp.bakery`
- **বিল্ড**: `./gradlew build` · **পরীক্ষা**: `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **কভারেজ**: ≥ 85 % (Kover `koverThresholdCheck`, `check`-এ যুক্ত) · **Cucumber**: 129/129 PASS

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## এটি কী করে

`bakery-gradle` **JBake 2.7.0** (Thymeleaf টেমপ্লেট, AsciiDoc/Markdown কন্টেন্ট)-এর মাধ্যমে একটি স্ট্যাটিক সাইট জেনারেট এবং ডিপ্লয় করে। এটি Firebase Auth + মন্তব্য, Google Forms, Analytics/Newsletter, একটি থিমিং সিস্টেম, অ্যাক্সেসিবিলিটি অডিট (WCAG/RGAA), ১০টি ভাষায় i18n মাইগ্রেশন, AI-সহায়ক প্রবন্ধ/স্ক্যাফোল্ড/থিম জেনারেশন (LangChain4j + Ollama), এবং runner-gradle-এর N3 কম্পোজিট কন্টেক্সটকে পরিচালিত করা **LENtille** সংবর্ধিত-কন্টেক্সট প্যাটার্ন এম্বেড করে। `publishSite` অ্যাগ্রিগেট টাস্ক এক কমান্ডে বেক করে এবং `gh-pages`-এ ডিপ্লয় করে।

CCCP Education মাল্টি-প্লাগইন ইকোসিস্টেমের অংশ:

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                 ↑
                     codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## দ্রুত শুরু

### ১. প্লাগইন প্রয়োগ করুন

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### ২. DSL বা `site.yml` এর মাধ্যমে কনফিগার করুন

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

`site.yml` (ভার্সন কন্ট্রোলে নেই — ব্যাকআপ ছাড়া কখনো ওভাররাইট করবেন না) JBake, ডিপ্লয়, Firebase, Analytics, থিম, লেআউট এবং সংবর্ধিত-কন্টেক্সট কনফিগারেশন নিয়ন্ত্রণ করে।

### ৩. বেক এবং ডিপ্লয় করুন

```bash
./gradlew publishSite        # অ্যাগ্রিগেট: bake + gh-pages-এ ডিপ্লয়
./gradlew bake               # শুধু JBake রেন্ডার
./gradlew deploySite          # বেক করা আউটপুট gh-pages-এ পুশ করুন
```

## উপলব্ধ টাস্ক

| টাস্ক | গ্রুপ | বিবরণ |
|------|-------|-------------|
| `bake`                      | —        | JBake রেন্ডার (`configureBakeTask` দ্বারা কনফিগার করা) |
| `generateSite`              | generate | সাইট + maquette ফোল্ডার শুরু করুন (প্রকার: `blog`/`basic`) |
| `generateSiteFromIntention` | generate | AI-সহায়ক সাইট স্ক্যাফোল্ড (ইন্টারঅ্যাক্টিভ, Ollama) |
| `generateTheme`             | generate | ক্যাটালগ থেকে থিম জেনারেট করুন (ভ্যারিয়েন্ট + ওভাররাইড) |
| `generateArticle`           | generate | AI-সহায়ক ব্লগ প্রবন্ধ → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | একটি বিদ্যমান bakery সাইটকে i18n-এ মাইগ্রেট করুন (টেমপ্লেট স্ক্যান করুন, হার্ডকোডেড টেক্সট বের করুন, `messages_{lang}.properties` জেনারেট করুন) |
| `pagefind`                  | transform | পূর্ণ-টেক্সট সার্চের জন্য Pagefind দিয়ে বেক করা সাইট ইনডেক্স করুন |
| `deploySite`                | deploy    | বেক করা আউটপুট GitHub `gh-pages` ব্রাঞ্চে পুশ করুন (JGit) |
| `deployMaquette`            | deploy    | maquette ফাইলগুলো রিপোজিটরিতে পুশ করুন |
| `deployProfile`             | deploy    | প্রোফাইল ফাইল (যেমন README.md) GitHub রিপোজিটরিতে পুশ করুন |
| `publishSite`               | publish   | **অ্যাগ্রিগেট** — bake + `gh-pages` ডিপ্লয় (সুবিধা) |
| `collectSiteConfig`         | collect   | Bakery কনফিগারেশন শুরু করুন |
| `collectSiteContext`        | collect   | বেক করা সাইট কন্টেক্সট সংগ্রহ করুন → `build/bakery/metadata.json` (runner-gradle N3-এর জন্য) |
| `collectAugmentedContext`   | collect   | LENS সংবর্ধিত কন্টেক্সট সংগ্রহ করুন (segregation + enrichment + budget) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | বেক করা সাইট স্থানীয়ভাবে পরিবেশন করুন (JavaExec) |
| `validateFirebaseConfig`   | validate  | Firebase কনফিগারেশন সামঞ্জস্য যাচাই করুন (মেকানিক্যাল + ঐচ্ছিক IA) |
| `verifyConfigurationMapping`| verification | YAML `site.yml` → `SiteConfiguration` ম্যাপিং যাচাই করুন এবং সিক্রেট মাস্ক করুন |
| `accessibilityAudit`       | audit     | বেক করা সাইটের WCAG/RGAA অ্যাক্সেসিবিলিটি অডিট — JSON/ASCII রিপোর্ট |
| `installPlaywright`         | verification | E2E পরীক্ষার জন্য Playwright Chromium ব্রাউজার ইনস্টল করুন |

> **ডিগ্রেডেশন মোড**: যখন `configPath` অনুপস্থিত/অবৈধ, প্লাগইন **শুধু-স্ক্যাফোল্ড** মোডে ফিরে যায় (`generateSite`, `generateSiteFromIntention`, `deployProfile`)। DSL, `gradle.properties` (`bakery.config.path=site.yml`), বা `-Pbakery.config.path=...` এর মাধ্যমে `configPath` সেট করুন।

## এক্সটেনশন DSL

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

## পূর্বশর্ত

- **Java** 24+ (Kotlin 2.3.20 টুলচেইন)
- **Gradle** 9.5.1+
- **Node.js** (Gradle Node প্লাগইন `7.1.0`-এর মাধ্যমে Pagefind + Playwright-এর জন্য)
- **Ollama** পোর্ট `11437–11465`-এ (পোর্ট `11434–11436` নিষিদ্ধ) — মডেল `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (শুধু Maven Central-এ প্রকাশনার জন্য)

## বিল্ড এবং পরীক্ষা

```bash
./gradlew build                     # পূর্ণ বিল্ড (UT + functionalTest + Cucumber)
./gradlew test                      # JUnit5 ইউনিট পরীক্ষা (Cucumber ইঞ্জিন বাদ দেয়)
./gradlew functionalTest            # GradleTestKit ফাংশনাল পরীক্ষা
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # Playwright E2E (Chromium ইনস্টল করে)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # কভারেজ ≥ 85 %
./gradlew publishToMavenLocal       # স্থানীয় প্রকাশনা
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## সমস্যা সমাধান

| লক্ষণ | সমাধান |
|---------|-----|
| `configPath is not set`          | `bakery { configPath = "site.yml" }`, `gradle.properties`, বা `-Pbakery.config.path=...` সেট করুন |
| `Failed to read site.yml`        | ফাইল বিদ্যমান এবং বৈধ YAML কিনা যাচাই করুন; প্লাগইন শুধু-স্ক্যাফোল্ডে ফিরে যায় |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Playwright Chromium অনুপস্থিত      | `./gradlew installPlaywright` (`e2eTest` দ্বারা স্বয়ংক্রিয়ভাবে চালিত) |
| Ollama সংযোগ প্রত্যাখ্যাত        | `11437–11465`-এ পোর্ট যাচাই করুন; `11434–11436` নিষিদ্ধ |
| gh-pages পুশ প্রত্যাখ্যাত             | JGit SSH কী + `site.yml` pushPage কনফিগ যাচাই করুন |

গভর্ন্যান্সের জন্য [AGENT.adoc](../bakery-plugin/AGENT.adoc) এবং প্রজেক্ট কন্টেক্সটের জন্য [README.adoc](../README.adoc) দেখুন।

## লাইসেন্স

Apache License 2.0 — [LICENSE](../bakery-plugin/LICENCE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_
<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — उपभोक्ता गाइड

> स्थैतिक साइट निर्माण के लिए Gradle प्लगइन — JBake + Thymeleaf + Firebase Auth + Analytics + नॉलेज ग्राफ + LLM।

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **संस्करण**: `0.0.2` · **Group**: `education.ccp` · **प्लगइन ID**: `education.ccp.bakery`
- **बिल्ड**: `./gradlew build` · **परीक्षण**: `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **कवरेज**: ≥ 85 % (Kover `koverThresholdCheck`, `check` में जुड़ा हुआ) · **Cucumber**: 129/129 PASS

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## यह क्या करता है

`bakery-gradle` **JBake 2.7.0** (Thymeleaf टेम्पलेट, AsciiDoc/Markdown सामग्री) के माध्यम से एक स्थैतिक साइट बनाता और तैनात करता है। यह Firebase Auth + टिप्पणियाँ, Google Forms, Analytics/Newsletter, एक थीमिंग सिस्टम, पहुँच लेखा परीक्षण (WCAG/RGAA), 10 भाषाओं में i18n माइग्रेशन, AI-सहायता प्राप्त लेख/स्कैफ़ोल्ड/थीम निर्माण (LangChain4j + Ollama), और runner-gradle के N3 कम्पोज़िट संदर्भ को खिलाने वाला **LENtille** संवर्धित-संदर्भ पैटर्न एम्बेड करता है। `publishSite` एग्रीगेट कार्य एक ही कमांड में बेक और `gh-pages` पर तैनाती करता है।

CCCP Education मल्टी-प्लगइन इकोसिस्टम का हिस्सा:

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                 ↑
                     codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## त्वरित प्रारंभ

### 1. प्लगइन लागू करें

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. DSL या `site.yml` द्वारा कॉन्फ़िगर करें

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

`site.yml` (संस्करणित नहीं — बिना बैकअप के कभी अधिलेखित न करें) JBake, तैनाती, Firebase, Analytics, थीम, लेआउट, और संवर्धित-संदर्भ कॉन्फ़िगरेशन को नियंत्रित करता है।

### 3. बेक और तैनात करें

```bash
./gradlew publishSite        # एग्रीगेट: bake + gh-pages पर तैनाती
./gradlew bake               # केवल JBake रेंडर
./gradlew deploySite          # बेक किए गए आउटपुट को gh-pages पर धकेलें
```

## उपलब्ध कार्य

| कार्य | समूह | विवरण |
|------|-------|-------------|
| `bake`                      | —        | JBake रेंडर (`configureBakeTask` द्वारा कॉन्फ़िगर किया गया) |
| `generateSite`              | generate | साइट + maquette फ़ोल्डर आरंभ करें (प्रकार: `blog`/`basic`) |
| `generateSiteFromIntention` | generate | AI-सहायता प्राप्त साइट स्कैफ़ोल्ड (इंटरैक्टिव, Ollama) |
| `generateTheme`             | generate | कैटलॉग से थीम उत्पन्न करें (वेरिएंट + ओवरराइड) |
| `generateArticle`           | generate | AI-सहायता प्राप्त ब्लॉग लेख → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | किसी मौजूदा bakery साइट को i18n में माइग्रेट करें (टेम्पलेट स्कैन करें, हार्डकोडेड टेक्स्ट निकालें, `messages_{lang}.properties` उत्पन्न करें) |
| `pagefind`                  | transform | पूर्ण-टेक्स्ट खोज के लिए बेक की गई साइट को Pagefind से अनुक्रमित करें |
| `deploySite`                | deploy    | बेक किए गए आउटपुट को GitHub `gh-pages` ब्रांच पर धकेलें (JGit) |
| `deployMaquette`            | deploy    | maquette फ़ाइलों को रिपॉज़िटरी में धकेलें |
| `deployProfile`             | deploy    | profile फ़ाइलों (जैसे README.md) को GitHub रिपॉज़िटरी में धकेलें |
| `publishSite`               | publish   | **एग्रीगेट** — bake + `gh-pages` तैनाती (सुविधा) |
| `collectSiteConfig`         | collect   | Bakery कॉन्फ़िगरेशन आरंभ करें |
| `collectSiteContext`        | collect   | बेक की गई साइट संदर्भ एकत्र करें → `build/bakery/metadata.json` (runner-gradle N3 के लिए) |
| `collectAugmentedContext`   | collect   | LENS संवर्धित संदर्भ एकत्र करें (segregation + enrichment + budget) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | बेक की गई साइट को स्थानीय रूप से सर्व करें (JavaExec) |
| `validateFirebaseConfig`   | validate  | Firebase कॉन्फ़िगरेशन सुसंगति सत्यापित करें (यांत्रिक + वैकल्पिक IA) |
| `verifyConfigurationMapping`| verification | YAML `site.yml` → `SiteConfiguration` मैपिंग सत्यापित करें और रहस्यों को छिपाएँ |
| `accessibilityAudit`       | audit     | बेक की गई साइट का WCAG/RGAA पहुँच लेखा — JSON/ASCII रिपोर्ट |
| `installPlaywright`         | verification | E2E परीक्षणों के लिए Playwright Chromium ब्राउज़र स्थापित करें |

> **डिग्रेडेशन मोड**: जब `configPath` अनुपस्थित/अमान्य हो, प्लगइन **केवल-स्कैफ़ोल्ड** मोड में गिर जाता है (`generateSite`, `generateSiteFromIntention`, `deployProfile`)। DSL, `gradle.properties` (`bakery.config.path=site.yml`), या `-Pbakery.config.path=...` द्वारा `configPath` सेट करें।

## एक्सटेंशन DSL

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

## पूर्वापेक्षाएँ

- **Java** 24+ (Kotlin 2.3.20 टूलचेन)
- **Gradle** 9.5.1+
- **Node.js** (Gradle Node प्लगइन `7.1.0` के माध्यम से Pagefind + Playwright के लिए)
- **Ollama** पोर्ट `11437–11465` पर (पोर्ट `11434–11436` निषिद्ध) — मॉडल `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (केवल Maven Central पर प्रकाशन के लिए)

## बिल्ड और परीक्षण

```bash
./gradlew build                     # पूर्ण बिल्ड (UT + functionalTest + Cucumber)
./gradlew test                      # JUnit5 यूनिट परीक्षण (Cucumber इंजन को बाहर रखता है)
./gradlew functionalTest            # GradleTestKit फंक्शनल परीक्षण
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # Playwright E2E (Chromium स्थापित करता है)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # कवरेज ≥ 85 %
./gradlew publishToMavenLocal       # स्थानीय प्रकाशन
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## समस्या निवारण

| लक्षण | समाधान |
|---------|-----|
| `configPath is not set`          | `bakery { configPath = "site.yml" }`, `gradle.properties`, या `-Pbakery.config.path=...` सेट करें |
| `Failed to read site.yml`        | फ़ाइल मौजूद है और मान्य YAML है यह जाँचें; प्लगइन केवल-स्कैफ़ोल्ड पर गिर जाता है |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Playwright Chromium अनुपस्थित      | `./gradlew installPlaywright` (`e2eTest` द्वारा स्वतः चलता है) |
| Ollama कनेक्शन अस्वीकृत        | पोर्ट `11437–11465` में सत्यापित करें; `11434–11436` निषिद्ध हैं |
| gh-pages पुश अस्वीकृत             | JGit SSH कुंजी + `site.yml` pushPage कॉन्फ़िग जाँचें |

शासन के लिए [AGENT.adoc](../bakery-plugin/AGENT.adoc) और परियोजना संदर्भ के लिए [README.adoc](../README.adoc) देखें।

## लाइसेंस

Apache License 2.0 — [LICENSE](../bakery-plugin/LICENCE) देखें।

---

_CCCP Education इकोसिस्टम का हिस्सा — `groupId: education.cccp`।_
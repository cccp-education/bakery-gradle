<!-- master source — other languages are translations of this file -->
# bakery-gradle — Consumer Guide

> Gradle plugin for static site generation — JBake + Thymeleaf + Firebase Auth + Analytics + Knowledge Graph + LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **Version**: `0.0.2` · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.bakery`
- **Build**: `./gradlew build` · **Tests**: `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **Coverage**: ≥ 85 % (Kover `koverThresholdCheck`, wired into `check`) · **Cucumber**: 129/129 PASS

🌐 Languages: **EN** | [中文](README.consommateurs/README.zh.md) | [हिन्दी](README.consommateurs/README.hi.md) | [Español](README.consommateurs/README.es.md) | [Français](README.consommateurs/README.fr.md) | [العربية](README.consommateurs/README.ar.md) | [বাংলা](README.consommateurs/README.bn.md) | [Português](README.consommateurs/README.pt.md) | [Русский](README.consommateurs/README.ru.md) | [اردو](README.consommateurs/README.ur.md)

---

## What it does

`bakery-gradle` generates and deploys a static site via **JBake 2.7.0** (Thymeleaf templates,
AsciiDoc/Markdown content). It embeds Firebase Auth + comments, Google Forms, Analytics/Newsletter,
a theming system, accessibility audit (WCAG/RGAA), i18n migration to 10 languages, AI-assisted
article/scaffold/theme generation (LangChain4j + Ollama), and the **LENtille** augmented-context
pattern feeding the N3 composite context for runner-gradle. The `publishSite` aggregate task bakes
and deploys to `gh-pages` in one command.

Part of the CCCP Education multi-plugin ecosystem:

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                ↑
                    codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## Quick Start

### 1. Apply the plugin

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. Configure via DSL or `site.yml`

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

`site.yml` (unversioned — never overwrite without backup) drives JBake, deploy, Firebase,
analytics, theme, layout, and augmented-context configuration.

### 3. Bake and deploy

```bash
./gradlew publishSite        # aggregate: bake + deploy gh-pages
./gradlew bake               # JBake render only
./gradlew deploySite          # push baked output to gh-pages
```

## Available tasks

| Task | Group | Description |
|------|-------|-------------|
| `bake`                      | —        | JBake render (configured by `configureBakeTask`) |
| `generateSite`              | generate | Initialise site + maquette folders (type: `blog`/`basic`) |
| `generateSiteFromIntention` | generate | AI-assisted site scaffold (interactive, Ollama) |
| `generateTheme`             | generate | Generate a theme from the catalogue (variant + overrides) |
| `generateArticle`           | generate | AI-assisted blog article → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | Migrate an existing bakery site to i18n (scan templates, extract hardcoded text, generate `messages_{lang}.properties`) |
| `pagefind`                  | transform | Index the baked site with Pagefind for full-text search |
| `deploySite`                | deploy    | Push baked output to GitHub `gh-pages` branch (JGit) |
| `deployMaquette`            | deploy    | Push maquette files to repository |
| `deployProfile`             | deploy    | Push profile files (e.g. README.md) to GitHub repository |
| `publishSite`               | publish   | **Aggregate** — bake + deploy `gh-pages` (convenience) |
| `collectSiteConfig`         | collect   | Initialize Bakery configuration |
| `collectSiteContext`        | collect   | Collect baked site context → `build/bakery/metadata.json` for runner-gradle N3 |
| `collectAugmentedContext`   | collect   | Collect LENS augmented context (segregation + enrichment + budget) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | Serve the baked site locally (JavaExec) |
| `validateFirebaseConfig`   | validate  | Validate Firebase configuration coherence (mechanical + optional IA) |
| `verifyConfigurationMapping`| verification | Validate YAML `site.yml` → `SiteConfiguration` mapping and mask secrets |
| `accessibilityAudit`       | audit     | WCAG/RGAA accessibility audit of the baked site — JSON/ASCII report |
| `installPlaywright`         | verification | Install Playwright Chromium browser for E2E tests |

> **Mode degradation**: when `configPath` is absent/invalid, the plugin falls back to
> **scaffold-only** mode (`generateSite`, `generateSiteFromIntention`, `deployProfile`). Set
> `configPath` via DSL, `gradle.properties` (`bakery.config.path=site.yml`), or `-Pbakery.config.path=...`.

## Extension DSL

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

## Prerequisites

- **Java** 24+ (Kotlin 2.3.20 toolchain)
- **Gradle** 9.5.1+
- **Node.js** (for Pagefind + Playwright via Gradle Node plugin `7.1.0`)
- **Ollama** on port `11437–11465` (ports `11434–11436` forbidden) — models `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (only for publishing to Maven Central)

## Build & test

```bash
./gradlew build                     # full build (UT + functionalTest + Cucumber)
./gradlew test                      # JUnit5 unit tests (excludes Cucumber engine)
./gradlew functionalTest            # GradleTestKit functional tests
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # Playwright E2E (installs Chromium)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # coverage ≥ 85 %
./gradlew publishToMavenLocal       # publish locally
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `configPath is not set`          | Set `bakery { configPath = "site.yml" }`, `gradle.properties`, or `-Pbakery.config.path=...` |
| `Failed to read site.yml`        | Check file exists and is valid YAML; plugin falls back to scaffold-only |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Playwright Chromium missing      | `./gradlew installPlaywright` (auto-run by `e2eTest`) |
| Ollama connection refused        | Verify port in `11437–11465`; `11434–11436` are forbidden |
| gh-pages push denied             | Check JGit SSH key + `site.yml` pushPage config |

See [AGENT.adoc](../bakery-plugin/AGENT.adoc) for governance and [README.adoc](../README.adoc) for project context.

## License

Apache License 2.0 — see [LICENSE](../bakery-plugin/LICENCE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._
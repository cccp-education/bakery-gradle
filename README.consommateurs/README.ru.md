<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — Руководство потребителя

> Плагин Gradle для генерации статических сайтов — JBake + Thymeleaf + Firebase Auth + Analytics + Knowledge Graph + LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **Версия**: `0.0.2` · **Группа**: `education.ccp` · **ID плагина**: `education.ccp.bakery`
- **Сборка**: `./gradlew build` · **Тесты**: `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **Покрытие**: ≥ 85 % (Kover `koverThresholdCheck`, подключён к `check`) · **Cucumber**: 129/129 PASS

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Что делает

`bakery-gradle` генерирует и развёртывает статический сайт через **JBake 2.7.0** (шаблоны Thymeleaf,
содержимое AsciiDoc/Markdown). Он встраивает Firebase Auth + комментарии, Google Forms, Analytics/Newsletter,
систему тем, аудит доступности (WCAG/RGAA), миграцию i18n на 10 языков, генерацию статей/скаффолда/тем
с помощью ИИ (LangChain4j + Ollama) и паттерн расширенного контекста **LENtille**, питающий
составной контекст N3 для runner-gradle. Агрегатная задача `publishSite` выпекает и развёртывает
на `gh-pages` одной командой.

Часть мультиплагинной экосистемы CCCP Education:

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                 ↑
                     codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## Быстрый старт

### 1. Применить плагин

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. Настроить через DSL или `site.yml`

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

`site.yml` (не под версионным контролем — никогда не перезаписывать без резервной копии) управляет
JBake, развёртыванием, Firebase, аналитикой, темой, макетом и конфигурацией расширенного контекста.

### 3. Выпекать и развёртывать

```bash
./gradlew publishSite        # агрегат: bake + развёртывание на gh-pages
./gradlew bake               # только рендер JBake
./gradlew deploySite          # отправить выпеченный вывод на gh-pages
```

## Доступные задачи

| Задача | Группа | Описание |
|------|-------|-------------|
| `bake`                      | —        | Рендер JBake (настраивается через `configureBakeTask`) |
| `generateSite`              | generate | Инициализация папок сайта + maquette (тип: `blog`/`basic`) |
| `generateSiteFromIntention` | generate | AI-ассистируемый скаффолд сайта (интерактивный, Ollama) |
| `generateTheme`             | generate | Генерация темы из каталога (вариант + переопределения) |
| `generateArticle`           | generate | AI-ассистируемая статья блога → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | Миграция существующего сайта bakery на i18n (сканирование шаблонов, извлечение захардкоженного текста, генерация `messages_{lang}.properties`) |
| `pagefind`                  | transform | Индексация выпеченного сайта с помощью Pagefind для полнотекстового поиска |
| `deploySite`                | deploy    | Отправка выпеченного вывода в ветку `gh-pages` GitHub (JGit) |
| `deployMaquette`            | deploy    | Отправка файлов maquette в репозиторий |
| `deployProfile`             | deploy    | Отправка файлов профиля (напр. README.md) в репозиторий GitHub |
| `publishSite`               | publish   | **Агрегат** — bake + развёртывание `gh-pages` (удобство) |
| `collectSiteConfig`         | collect   | Инициализация конфигурации Bakery |
| `collectSiteContext`        | collect   | Сбор контекста выпеченного сайта → `build/bakery/metadata.json` для runner-gradle N3 |
| `collectAugmentedContext`   | collect   | Сбор расширенного контекста LENS (сегрегация + обогащение + бюджет) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | Локальное обслуживание выпеченного сайта (JavaExec) |
| `validateFirebaseConfig`   | validate  | Проверка согласованности конфигурации Firebase (механическая + опциональная IA) |
| `verifyConfigurationMapping`| verification | Проверка маппинга YAML `site.yml` → `SiteConfiguration` и сокрытие секретов |
| `accessibilityAudit`       | audit     | Аудит доступности WCAG/RGAA выпеченного сайта — отчёт JSON/ASCII |
| `installPlaywright`         | verification | Установка браузера Playwright Chromium для E2E-тестов |

> **Режим деградации**: когда `configPath` отсутствует/недействителен, плагин откатывается к
> режиму **только скаффолд** (`generateSite`, `generateSiteFromIntention`, `deployProfile`). Задайте
> `configPath` через DSL, `gradle.properties` (`bakery.config.path=site.yml`) или `-Pbakery.config.path=...`.

## DSL расширения

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

## Предварительные требования

- **Java** 24+ (тулчейн Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **Node.js** (для Pagefind + Playwright через плагин Gradle Node `7.1.0`)
- **Ollama** на порту `11437–11465` (порты `11434–11436` запрещены) — модели `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (только для публикации в Maven Central)

## Сборка и тесты

```bash
./gradlew build                     # полная сборка (UT + functionalTest + Cucumber)
./gradlew test                      # модульные тесты JUnit5 (исключает движок Cucumber)
./gradlew functionalTest            # функциональные тесты GradleTestKit
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # Playwright E2E (устанавливает Chromium)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # покрытие ≥ 85 %
./gradlew publishToMavenLocal       # локальная публикация
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## Устранение неисправностей

| Симптом | Решение |
|---------|-----|
| `configPath is not set`          | Задайте `bakery { configPath = "site.yml" }`, `gradle.properties` или `-Pbakery.config.path=...` |
| `Failed to read site.yml`        | Проверьте существование файла и корректность YAML; плагин откатится к только скаффолду |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Playwright Chromium отсутствует      | `./gradlew installPlaywright` (автозапуск через `e2eTest`) |
| Ollama connection refused        | Проверьте порт в `11437–11465`; `11434–11436` запрещены |
| gh-pages push denied             | Проверьте SSH-ключ JGit + конфигурацию pushPage в `site.yml` |

См. [AGENT.adoc](../bakery-plugin/AGENT.adoc) для управления и [README.adoc](../README.adoc) для контекста проекта.

## Лицензия

Apache License 2.0 — см. [LICENSE](../bakery-plugin/LICENCE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._
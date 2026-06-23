<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — Guía del consumidor

> Plugin de Gradle para la generación de sitios estáticos — JBake + Thymeleaf + Firebase Auth + Analytics + Knowledge Graph + LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **Versión**: `0.0.2` · **Group**: `education.ccp` · **ID del plugin**: `education.ccp.bakery`
- **Build**: `./gradlew build` · **Tests**: `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **Cobertura**: ≥ 85 % (Kover `koverThresholdCheck`, integrado en `check`) · **Cucumber**: 129/129 PASS

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Qué hace

`bakery-gradle` genera y despliega un sitio estático mediante **JBake 2.7.0** (plantillas Thymeleaf,
contenido AsciiDoc/Markdown). Integra Firebase Auth + comentarios, Google Forms, Analytics/Newsletter,
un sistema de temas, auditoría de accesibilidad (WCAG/RGAA), migración i18n a 10 idiomas, generación
de artículos/scaffold/tema asistida por IA (LangChain4j + Ollama) y el patrón de contexto aumentado
**LENtille** que alimenta el contexto compuesto N3 de runner-gradle. La tarea agregada `publishSite`
compila y despliega a `gh-pages` en un solo comando.

Parte del ecosistema multi-plugin de CCCP Education:

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                 ↑
                     codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## Inicio rápido

### 1. Aplicar el plugin

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. Configurar mediante DSL o `site.yml`

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

`site.yml` (sin versionar — nunca sobrescribir sin copia de seguridad) controla JBake, despliegue,
Firebase, analytics, tema, diseño y la configuración de contexto aumentado.

### 3. Compilar y desplegar

```bash
./gradlew publishSite        # agregado: bake + despliegue a gh-pages
./gradlew bake               # solo render JBake
./gradlew deploySite          # enviar la salida compilada a gh-pages
```

## Tareas disponibles

| Tarea | Grupo | Descripción |
|------|-------|-------------|
| `bake`                      | —        | Render JBake (configurado por `configureBakeTask`) |
| `generateSite`              | generate | Inicializar carpetas de sitio + maquette (tipo: `blog`/`basic`) |
| `generateSiteFromIntention` | generate | Scaffold de sitio asistido por IA (interactivo, Ollama) |
| `generateTheme`             | generate | Generar un tema del catálogo (variante + overrides) |
| `generateArticle`           | generate | Artículo de blog asistido por IA → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | Migrar un sitio bakery existente a i18n (escanear plantillas, extraer texto codificado, generar `messages_{lang}.properties`) |
| `pagefind`                  | transform | Indexar el sitio compilado con Pagefind para búsqueda de texto completo |
| `deploySite`                | deploy    | Enviar la salida compilada a la rama `gh-pages` de GitHub (JGit) |
| `deployMaquette`            | deploy    | Enviar archivos maquette al repositorio |
| `deployProfile`             | deploy    | Enviar archivos de perfil (p. ej. README.md) al repositorio GitHub |
| `publishSite`               | publish   | **Agregado** — bake + despliegue `gh-pages` (conveniencia) |
| `collectSiteConfig`         | collect   | Inicializar la configuración de Bakery |
| `collectSiteContext`        | collect   | Recopilar contexto del sitio compilado → `build/bakery/metadata.json` para runner-gradle N3 |
| `collectAugmentedContext`   | collect   | Recopilar contexto aumentado LENS (segregación + enriquecimiento + presupuesto) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | Servir el sitio compilado localmente (JavaExec) |
| `validateFirebaseConfig`   | validate  | Validar la coherencia de la configuración Firebase (mecánica + IA opcional) |
| `verifyConfigurationMapping`| verification | Validar el mapeo YAML `site.yml` → `SiteConfiguration` y ocultar secretos |
| `accessibilityAudit`       | audit     | Auditoría de accesibilidad WCAG/RGAA del sitio compilado — informe JSON/ASCII |
| `installPlaywright`         | verification | Instalar el navegador Chromium de Playwright para pruebas E2E |

> **Modo degradado**: cuando `configPath` está ausente/no válida, el plugin recurre al
> modo **solo scaffold** (`generateSite`, `generateSiteFromIntention`, `deployProfile`). Establezca
> `configPath` mediante DSL, `gradle.properties` (`bakery.config.path=site.yml`) o `-Pbakery.config.path=...`.

## DSL de extensión

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

## Requisitos previos

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **Node.js** (para Pagefind + Playwright vía el plugin Gradle Node `7.1.0`)
- **Ollama** en el puerto `11437–11465` (puertos `11434–11436` prohibidos) — modelos `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (solo para publicar en Maven Central)

## Build y test

```bash
./gradlew build                     # build completo (UT + functionalTest + Cucumber)
./gradlew test                      # tests unitarios JUnit5 (excluye el motor Cucumber)
./gradlew functionalTest            # tests funcionales GradleTestKit
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # Playwright E2E (instala Chromium)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # cobertura ≥ 85 %
./gradlew publishToMavenLocal       # publicación local
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## Solución de problemas

| Síntoma | Solución |
|---------|-----|
| `configPath is not set`          | Establezca `bakery { configPath = "site.yml" }`, `gradle.properties` o `-Pbakery.config.path=...` |
| `Failed to read site.yml`        | Verifique que el archivo existe y es YAML válido; el plugin recurre a solo scaffold |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Chromium de Playwright ausente      | `./gradlew installPlaywright` (ejecutado automáticamente por `e2eTest`) |
| Conexión Ollama rechazada        | Verifique el puerto en `11437–11465`; `11434–11436` están prohibidos |
| push a gh-pages denegado             | Revise la clave SSH de JGit + configuración pushPage de `site.yml` |

Consulte [AGENT.adoc](../bakery-plugin/AGENT.adoc) para la gobernanza y [README.adoc](../README.adoc) para el contexto del proyecto.

## Licencia

Apache License 2.0 — consulte [LICENSE](../bakery-plugin/LICENCE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._
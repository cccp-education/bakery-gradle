<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — Guia do consumidor

> Plugin Gradle para geração de sites estáticos — JBake + Thymeleaf + Firebase Auth + Analytics + Knowledge Graph + LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **Versão**: `0.0.2` · **Grupo**: `education.ccp` · **ID do plugin**: `education.ccp.bakery`
- **Build**: `./gradlew build` · **Testes**: `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **Cobertura**: ≥ 85 % (Kover `koverThresholdCheck`, integrado em `check`) · **Cucumber**: 129/129 PASS

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## O que faz

O `bakery-gradle` gera e implanta um site estático via **JBake 2.7.0** (modelos Thymeleaf,
conteúdo AsciiDoc/Markdown). Ele embute Firebase Auth + comentários, Google Forms, Analytics/Newsletter,
um sistema de temas, auditoria de acessibilidade (WCAG/RGAA), migração i18n para 10 idiomas, geração
de artigo/scaffold/tema assistida por IA (LangChain4j + Ollama) e o padrão de contexto aumentado
**LENtille** que alimenta o contexto composto N3 do runner-gradle. A tarefa agregada `publishSite`
compila e implanta para `gh-pages` em um único comando.

Parte do ecossistema multi-plugin CCCP Education:

```
content (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                 ↑
                     codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## Início rápido

### 1. Aplicar o plugin

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. Configurar via DSL ou `site.yml`

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

`site.yml` (não versionado — nunca sobrescrever sem backup) controla JBake, implantação, Firebase,
analytics, tema, layout e configuração de contexto aumentado.

### 3. Compilar e implantar

```bash
./gradlew publishSite        # agregado: bake + implantação para gh-pages
./gradlew bake               # apenas render JBake
./gradlew deploySite          # enviar a saída compilada para gh-pages
```

## Tarefas disponíveis

| Tarefa | Grupo | Descrição |
|------|-------|-------------|
| `bake`                      | —        | Render JBake (configurado por `configureBakeTask`) |
| `generateSite`              | generate | Inicializar pastas do site + maquette (tipo: `blog`/`basic`) |
| `generateSiteFromIntention` | generate | Scaffold de site assistido por IA (interativo, Ollama) |
| `generateTheme`             | generate | Gerar um tema a partir do catálogo (variante + overrides) |
| `generateArticle`           | generate | Artigo de blog assistido por IA → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | Migrar um site bakery existente para i18n (escanear modelos, extrair texto codificado, gerar `messages_{lang}.properties`) |
| `pagefind`                  | transform | Indexar o site compilado com Pagefind para busca de texto completo |
| `deploySite`                | deploy    | Enviar a saída compilada para o branch `gh-pages` do GitHub (JGit) |
| `deployMaquette`            | deploy    | Enviar arquivos maquette para o repositório |
| `deployProfile`             | deploy    | Enviar arquivos de perfil (ex.: README.md) para o repositório GitHub |
| `publishSite`               | publish   | **Agregado** — bake + implantação `gh-pages` (conveniência) |
| `collectSiteConfig`         | collect   | Inicializar configuração do Bakery |
| `collectSiteContext`        | collect   | Coletar contexto do site compilado → `build/bakery/metadata.json` para runner-gradle N3 |
| `collectAugmentedContext`   | collect   | Coletar contexto aumentado LENS (segregação + enriquecimento + orçamento) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | Servir o site compilado localmente (JavaExec) |
| `validateFirebaseConfig`   | validate  | Validar a coerência da configuração Firebase (mecânica + IA opcional) |
| `verifyConfigurationMapping`| verification | Validar mapeamento YAML `site.yml` → `SiteConfiguration` e ocultar segredos |
| `accessibilityAudit`       | audit     | Auditoria de acessibilidade WCAG/RGAA do site compilado — relatório JSON/ASCII |
| `installPlaywright`         | verification | Instalar o navegador Chromium do Playwright para testes E2E |

> **Modo de degradação**: quando `configPath` está ausente/inválido, o plugin recorre ao
> modo **somente scaffold** (`generateSite`, `generateSiteFromIntention`, `deployProfile`). Defina
> `configPath` via DSL, `gradle.properties` (`bakery.config.path=site.yml`) ou `-Pbakery.config.path=...`.

## DSL de extensão

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

## Pré-requisitos

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **Node.js** (para Pagefind + Playwright via plugin Gradle Node `7.1.0`)
- **Ollama** na porta `11437–11465` (portas `11434–11436` proibidas) — modelos `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (apenas para publicação no Maven Central)

## Build e teste

```bash
./gradlew build                     # build completo (UT + functionalTest + Cucumber)
./gradlew test                      # testes de unidade JUnit5 (exclui motor Cucumber)
./gradlew functionalTest            # testes funcionais GradleTestKit
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # Playwright E2E (instala Chromium)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # cobertura ≥ 85 %
./gradlew publishToMavenLocal       # publicação local
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## Solução de problemas

| Sintoma | Correção |
|---------|-----|
| `configPath is not set`          | Defina `bakery { configPath = "site.yml" }`, `gradle.properties` ou `-Pbakery.config.path=...` |
| `Failed to read site.yml`        | Verifique se o arquivo existe e é YAML válido; o plugin recorre a somente scaffold |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Chromium do Playwright ausente      | `./gradlew installPlaywright` (executado automaticamente por `e2eTest`) |
| Conexão Ollama recusada        | Verifique a porta em `11437–11465`; `11434–11436` são proibidas |
| push para gh-pages negado             | Verifique a chave SSH do JGit + configuração pushPage do `site.yml` |

Consulte [AGENT.adoc](../bakery-plugin/AGENT.adoc) para governança e [README.adoc](../README.adoc) para contexto do projeto.

## Licença

Apache License 2.0 — consulte [LICENSE](../bakery-plugin/LICENCE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._
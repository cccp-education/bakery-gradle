<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — Internos do plugin

> Guia para desenvolvedores e colaboradores do plugin Gradle `bakery-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=%E2%89%A585%25&color=green)]()
[![License](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=License)](../LICENSE)

- **Versão**: `0.0.2` · **Grupo**: `education.ccp` · **ID do plugin**: `education.ccp.bakery`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Build**: `./gradlew build` · **Testes**: `./gradlew check` · **Portão de cobertura**: `./gradlew koverThresholdCheck` (≥85 %)

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Layout de módulos

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # Ponto de entrada do plugin — registra todas as tarefas (ramificação afterEvaluate)
│   │   ├── BakeryExtension.kt           # Extensão DSL (configPath, siteType, language, ia, themes, a11y, …)
│   │   ├── BakeryConstants.kt           # Grupos de tarefas (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # Modelo site.yml (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (agregado)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # Acessibilidade WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille: segregação + enriquecimento + orçamento)
│   │   ├── article/ theme/ scaffold/   # DSLs de intenção (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # Contratos N0 refletidos localmente (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # Testes funcionais GradleTestKit
├── src/e2eTest/kotlin/bakery/e2e/       # Testes E2E Playwright
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # Catálogo de versões
```

## Contratos N0 (de workspace-bom MEMPHIS)

| Contrato | Artefato | Fornece |
|----------|----------|----------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(refletido em `contracts/i18n/`)_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(refletido em `contracts/pipeline/`)_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

O BOM é conectado via `implementation(platform("education.cccp:workspace-bom:0.0.1"))` em
`bakery-plugin/build.gradle.kts:29`.

## Dependências principais

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — motor de site estático (api)
- **Thymeleaf** `3.0.14.RELEASE` — renderização de modelos (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — implantação gh-pages (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — diagramas AsciiDoc (api, no bundle jbake)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — serviço LLM BKY-IA-0
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — assíncrono para OllamaLlmService
- **Arrow-kt** (core + fx-coroutines) — programação funcional (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — análise RSS (CS-8, substitui o frágil DocumentBuilderFactory DOM)
- **commons-io** `2.13.0` — utilitários de arquivo (api)
- **Node Gradle plugin** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — testes de navegador E2E
- **Jackson** (kotlin + yaml) — config YAML, serialização JSON
- **Graphify plugin** `0.0.2` — integração de Knowledge Graph (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — consumidor de RAG / contexto aumentado

## Instâncias Ollama (restrição global)

As portas `11434–11436` são proibidas. Rotacionar sobre `11437–11465` (29 portas).
Modelos autorizados: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Padrão do Bakery: `http://localhost:11464` + `gpt-oss:120b-cloud` (ver `IaConfig.kt`).

## Matriz de testes

| Tarefa | SourceSet | Escopo | Timeout | Paralelismo |
|------|-----------|-------|---------|-------------|
| `test`           | test          | Unidade JUnit5 (exclui motor Cucumber + `bakery.scenarios.*`) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | Funcional GradleTestKit | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Navegador Playwright (depende de `installPlaywright`) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | Cobertura ≥ 85 % (analisa XML do Kover) | — | — |

Cabeamento de verificação (`build.gradle.kts`):
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` **não** está cabeados em `check` — executar explicitamente via `./gradlew e2eTest`.
- Passos do Cucumber estão em `src/test/scenarios`, features em `src/test/features`.
- `testImplementation` estende da implementação de `functionalTest` (não o reverso).

## Ajuste JVM

- Todas as tarefas `Test`: `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest`: `maxHeapSize = "1g"`, limpa diretórios temporários `gradle-test-*` obsoletos com mais de 1h em `doLast`
- `e2eTest`: `maxParallelForks = 1`, `forkEvery = 1` (evitar conflitos de recursos do navegador)
- Kover: `includedSourceSets = ["main", "functionalTest"]`, relatórios HTML + XML em `check`

## Comandos de build

```bash
./gradlew build                                   # build completo (UT + functionalTest + Cucumber)
./gradlew build -x test                            # apenas compilar
./gradlew test                                     # testes de unidade JUnit5
./gradlew functionalTest                           # funcional GradleTestKit
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # Playwright E2E (auto-instalação Chromium)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # cobertura ≥ 85 %
./gradlew publishToMavenLocal                      # publicação local
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # instalar Chromium para E2E
```

> **Regra 0 (AGENT.adoc)**: `./gradlew -q publishToMavenLocal` é **obrigatório** após qualquer mudança de código-fonte,
> antes de qualquer teste funcional de um projeto consumidor.

## Pipeline CI

`.github/workflows/test.yml` (job único, `ubuntu-latest`, timeout 20 min, JDK 24 temurin):
1. `./gradlew build` (UT + Cucumber) — em `bakery-plugin/`
2. `./gradlew installPlaywright` — instala Chromium
3. `./gradlew e2eTest` — Playwright E2E

Workflows auxiliares: `website.yml` (implantação do site), `readme_plantuml.yml` (geração de README via readme-gradle).

## Publicação (NMCP)

Configurado via `com.gradleup.nmcp.settings` (`1.5.0`) em `bakery-plugin/settings.gradle.kts`.
Credenciais lidas de `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`),
`publishingType = "AUTOMATIC"`.
A assinatura usa `useGpgCmd()`; assinatura pulada quando `CI=true` ou versão termina com `-SNAPSHOT`.
O POM (em `withType<MavenPublication>`) declara:
- Licença Apache 2.0, desenvolvedor `cccp-education` (cccp.edu@gmail.com)
- SCM apontando para `github.com/cccp-education/bakery-gradle`
- Injeção opcional de `relocationGroup` para migração de groupId

Bloco `gradlePlugin`: `id = education.cccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName "Bakery Plugin", tags `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`.

Comando de publicação:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Status dos EPICs

Todos os EPICs fechados (ver `bakery-plugin/.agents/INDEX.adoc`):
- EPICs 1–13 (governança, Supabase→Firebase, build, formulário de contato, timeout Cucumber, publishProfile,
  tolerância YAML, release 0.1.4, scaffolding TDD, duração de testes, KG, cobertura) — ✅
- **BKY-JB-0→9** (especialista JBake: scaffold, Thymeleaf, Google Forms, Firebase Auth, Analytics,
  tema, layouts, artigo IA, E2E) — ✅
- **BKY-LENS-1→6** (LENtille: segregação, enriquecimento, orçamento, reescrita BDD, conexão N3, modelos aumentados) — ✅
- **BKY-IA-2** (tema IA paramétrico) — ✅
- **BKY-KOV-1** (Kover 78 % → 85.88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, sessão 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, sessão 143) — ✅
- **BKY-COV-2** (microfissuras DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 idiomas, 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (migração real magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (Lei da Economia da Tinta, 3/3 pts) — ✅
- **BKY-CR-V5** (60 code smells) — ✅
- **BKY-CR-V6** (3 duplicações estruturais, 8/8 pts) — ✅
- **BKY-A11Y-1** (acessibilidade WCAG/RGAA, 13/13 pts) — ✅
- **BKY-FIX-1** (deployProfile sem site.yml) — ✅

## Documentos de arquitetura

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — Regras absolutas e governança
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — Sessões, EPICs, roadmap
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — Trabalho restante
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — Missão da sessão atual
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — Roadmap especialista JBake
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — Revisão de código V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — Análise de cobertura
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Padrão Lazy/Eager
- [README.adoc](../README.adoc) — README a nível de projeto (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## Contribuindo

1. O build compila: `./gradlew build -x test`
2. Republicar localmente: `./gradlew -q publishToMavenLocal` (Regra 0, obrigatório)
3. Testes de unidade verdes: `./gradlew test`
4. Cucumber verde: `./gradlew cucumberTest` (129/129)
5. Cobertura respeitada: `./gradlew koverThresholdCheck` (≥ 85 %)
6. Seguir convenções DDD (value objects, ports/adapters, Arrow `Either`/`Raise`, sem vazamentos)
7. Nunca sobrescrever `site.yml` sem backup (AGENT.adoc §1b — regra de segurança)
8. Executar o procedimento de fechamento de 6 etapas (AGENT.adoc §3) ao fim da sessão

## Licença

Apache License 2.0 — ver [LICENCE](../bakery-plugin/LICENCE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._
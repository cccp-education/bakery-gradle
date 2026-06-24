<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — Internes du Plugin

> Guide développeur et contributeur pour le plugin Gradle `bakery-plugin`.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=Tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=couverture&message=%E2%89%A585%25&color=green)]()
[![Licence](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.2` · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.bakery`
- **Toolchain** : Java 24 · Kotlin 2.3.20 · Gradle 9.5.1
- **Build** : `./gradlew build` · **Tests** : `./gradlew check` · **Gate couverture** : `./gradlew koverThresholdCheck` (≥85 %)

🌐 Langues : [English](README.md) | **Français**

---

## Organisation des modules

```
bakery-plugin/
├── src/main/kotlin/
│   ├── bakery/
│   │   ├── BakeryPlugin.kt             # Point d'entrée du plugin — enregistre toutes les tâches (branchement afterEvaluate)
│   │   ├── BakeryExtension.kt           # DSL d'extension (configPath, siteType, language, ia, thèmes, a11y, …)
│   │   ├── BakeryConstants.kt           # Groupes de tâches (generate/deploy/publish/transform/info/collect/validate/audit)
│   │   ├── SiteConfiguration.kt         # Modèle site.yml (bake, pushMaquette, pushPage, pushProfile, …)
│   │   ├── SiteManager.kt / SiteScaffolder.kt / ConfigResolver.kt
│   │   ├── SiteTaskRegistrar.kt        # bake, generateSite, pagefind, serve, collectSiteConfig
│   │   ├── DeployTaskRegistrar.kt      # deploySite, deployMaquette, deployProfile, publishSite (agrégat)
│   │   ├── LensTaskRegistrar.kt        # collectSiteContext, collectAugmentedContext (LENtille)
│   │   ├── ContentTaskRegistrar.kt     # generateArticle, generateSiteFromIntention, generateTheme, migrateToI18n, validateFirebaseConfig
│   │   ├── a11y/                        # Accessibilité WCAG/RGAA (AuditTask, Auditor, ColorContrast, Dsl)
│   │   ├── llm/                         # LlmService, OllamaLlmService (LangChain4j), IaConfig
│   │   ├── i18n/                        # I18nMigrationService, LlmTranslationApplier, audit/FixtureAlignmentAuditor
│   │   ├── lens/                        # AugmentedContextDsl (LENtille : ségrégation + enrichissement + budget)
│   │   ├── article/ theme/ scaffold/   # DSL d'intention (BKY-JB-8, BKY-IA-2, BKY-IA-1)
│   │   ├── site/GenerateSiteService.kt
│   │   ├── ProfilePublisher.kt
│   │   └── util/Slugify.kt, JsonEscape.kt, AnalyticsDsl.kt, FirebaseAuthDsl.kt, GoogleFormsDsl.kt, ThemeDsl.kt, LayoutDsl.kt
│   └── contracts/                       # Contrats N0 dupliqués localement (i18n, pipeline)
│       ├── i18n/                        # SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle
│       └── pipeline/                    # ReleaseNotesGenerator, ConventionalCommit, GitLogParser, ReleaseNotesRenderer, ReleaseNotesConfig
├── src/functionalTest/kotlin/bakery/    # Tests fonctionnels GradleTestKit
├── src/e2eTest/kotlin/bakery/e2e/       # Tests E2E Playwright
├── src/test/{kotlin,features,scenarios} # JUnit5 + Cucumber BDD
└── gradle/libs.versions.toml            # Catalogue de versions
```

## Contrats N0 (depuis workspace-bom MEMPHIS)

| Contrat | Artefact | Fournit |
|---------|----------|---------|
| `codebase-contracts`  | `education.cccp:codebase-contracts:0.0.1` | ContextChannel, ChannelBudget, CompositeContext |
| `i18n-contracts`      | `education.cccp:i18n-contracts:0.0.1`     | SupportedLanguage, LanguageCatalog, I18nConfig, LocaleResolver, MessageBundle _(dupliqué dans `contracts/i18n/`)_ |
| `pipeline-contracts`  | `education.cccp:pipeline-contracts`       | ReleaseNotesGenerator, ConventionalCommit, GitLogParser _(dupliqué dans `contracts/pipeline/`)_ |
| `agent-contracts`     | `education.cccp:agent-contracts:0.0.1`    | Epic, UserStory, GradleTask, AgentState |
| `llm-pool-contracts`  | `education.cccp:llm-pool-contracts:0.0.1` | LlmInstancePool, LlmInstance, QuotaConfig |

La BOM est câblée via `implementation(platform("education.cccp:workspace-bom:0.0.1"))` dans
`bakery-plugin/build.gradle.kts:29`.

## Dépendances clés

- **JBake** `2.7.0` + `jbake-gradle-plugin` `5.5.0` — moteur de site statique (api)
- **Thymeleaf** `3.0.14.RELEASE` — rendu des templates (testImplementation + e2e)
- **JGit** `6.10.0` (core + ssh + archive) + **xz** `1.10` — déploiement gh-pages (api)
- **AsciidoctorJ Diagram** `3.0.1` + PlantUML `1.2026.2` — diagrammes AsciiDoc (api, dans le bundle jbake)
- **Firebase Auth + Comments** — BKY-JB-4 (maquette + site)
- **Analytics + Newsletter** — BKY-JB-5 (Plausible/Matomo)
- **LangChain4j + Ollama** (`langchain4j-ollama`) — service LLM BKY-IA-0
- **kotlinx-coroutines** `1.10.2` (core + jdk8) — async pour OllamaLlmService
- **Arrow-kt** (core + fx-coroutines) — programmation fonctionnelle (Either, Validation, Raise DSL)
- **Rome** `2.1.0` — parsing RSS (CS-8, remplace le DOM DocumentBuilderFactory fragile)
- **commons-io** `2.13.0` — utilitaires fichiers (api)
- **Plugin Gradle Node** `7.1.0` — Pagefind + Playwright (NpxTask)
- **Playwright** — tests E2E navigateur
- **Jackson** (kotlin + yaml) — config YAML, sérialisation JSON
- **Graphify plugin** `0.0.2` — intégration Knowledge Graph (LENtille)
- **codebase-plugin** `0.0.2` + **codebase-contracts** `0.0.1` — consommateur RAG / contexte augmenté

## Instances Ollama (contrainte globales)

Les ports `11434–11436` sont interdits. Rotation sur `11437–11465` (29 ports).
Modèles autorisés : `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
Défaut bakery : `http://localhost:11464` + `gpt-oss:120b-cloud` (voir `IaConfig.kt`).

## Matrice de tests

| Tâche | SourceSet | Portée | Timeout | Parallélisme |
|-------|-----------|-------|---------|--------------|
| `test`           | test          | JUnit5 unitaire (exclut moteur Cucumber + `bakery.scenarios.*`) | 5 min | maxParallelForks=2 |
| `functionalTest` | functionalTest | Fonctionnel GradleTestKit | — | = availableProcessors |
| `cucumberTest`   | test          | Cucumber BDD (129/129 PASS) | 5 min | 1 (forkEvery=0, maxHeap 1g) |
| `e2eTest`         | e2eTest       | Playwright navigateur (dépend de `installPlaywright`) | 10 min | 1 (forkEvery=1) |
| `koverThresholdCheck` | —       | Couverture ≥ 85 % (parse le XML Kover) | — | — |

Câblage de vérification (`build.gradle.kts`) :
- `tasks.check { dependsOn(functionalTestTask); dependsOn(cucumberTest) }`
- `tasks.check { dependsOn("koverThresholdCheck") }`
- `e2eTest` n'est **pas** câblé dans `check` — à lancer explicitement via `./gradlew e2eTest`.
- Les steps Cucumber sont dans `src/test/scenarios`, les features dans `src/test/features`.
- `testImplementation` hérite de `functionalTest` implementation (pas l'inverse).

## Réglage JVM

- Toutes les tâches `Test` : `jvmArgs("-XX:+EnableDynamicAgentLoading")`
- `cucumberTest` : `maxHeapSize = "1g"`, nettoie les répertoires temporaires `gradle-test-*` de plus d'1h dans `doLast`
- `e2eTest` : `maxParallelForks = 1`, `forkEvery = 1` (éviter les conflits de ressources navigateur)
- Kover : `includedSourceSets = ["main", "functionalTest"]`, rapports HTML + XML sur `check`

## Commandes de build

```bash
./gradlew build                                   # build complet (UT + functionalTest + Cucumber)
./gradlew build -x test                            # compile seulement
./gradlew test                                     # tests unitaires JUnit5
./gradlew functionalTest                           # fonctionnel GradleTestKit
./gradlew cucumberTest                             # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                                  # E2E Playwright (Chromium auto-install)
./gradlew check                                    # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck                      # couverture ≥ 85 %
./gradlew publishToMavenLocal                      # publication locale
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central (NMCP)
./gradlew installPlaywright                        # installer Chromium pour E2E
```

> **Règle 0 (AGENT.adoc)** : `./gradlew -q publishToMavenLocal` est **obligatoire** après toute modification
> du code source, avant tout test fonctionnel depuis un projet consommateur.

## Pipeline CI

`.github/workflows/test.yml` (job unique, `ubuntu-latest`, timeout 20 min, JDK 24 temurin) :
1. `./gradlew build` (UT + Cucumber) — dans `bakery-plugin/`
2. `./gradlew installPlaywright` — installe Chromium
3. `./gradlew e2eTest` — E2E Playwright

Workflows auxiliaires : `website.yml` (déploiement site), `readme_plantuml.yml` (génération README via readme-gradle).

## Publication (NMCP)

Configuré via `com.gradleup.nmcp.settings` (`1.5.0`) dans `bakery-plugin/settings.gradle.kts`.
Les identifiants sont lus depuis `~/.gradle/gradle.properties` (`ossrhUsername`, `ossrhPassword`),
`publishingType = "AUTOMATIC"`.
La signature utilise `useGpgCmd()` ; signature ignorée quand `CI=true` ou version en `-SNAPSHOT`.
Le POM (sur `withType<MavenPublication>`) déclare :
- Licence Apache 2.0, développeur `cccp-education` (cccp.edu@gmail.com)
- SCM pointant vers `github.com/cccp-education/bakery-gradle`
- Injection optionnelle de `relocationGroup` pour migration de groupId

Bloc `gradlePlugin` : `id = education.cccp.bakery`, `implementationClass = bakery.BakeryPlugin`,
displayName « Bakery Plugin », tags `[jbake, static-site-generator, blog, jgit, asciidoc, markdown, thymeleaf]`,
`website = https://cccp-education.github.io/`, `vcsUrl = https://github.com/cccp-education/bakery-gradle.git`.

Commande de publication :
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Statut des EPICs

Tous les EPICs clôturés (voir `bakery-plugin/.agents/INDEX.adoc`) :
- EPICs 1–13 (gouvernance, Supabase→Firebase, build, formulaire contact, timeout Cucumber, publishProfile,
  tolérance YAML, release 0.1.4, scaffolding TDD, durée tests, KG, couverture) — ✅
- **BKY-JB-0→9** (expert JBake : scaffold, Thymeleaf, Google Forms, Firebase Auth, Analytics,
  thème, layouts, article IA, E2E) — ✅
- **BKY-LENS-1→6** (LENtille : ségrégation, enrichissement, budget, réécriture BDD, connexion N3, templates augmentés) — ✅
- **BKY-IA-2** (thème IA paramétrique) — ✅
- **BKY-KOV-1** (Kover 78 % → 85,88 %) — ✅
- **BKY-PUB-1** (Maven Central 0.0.1, session 109) — ✅
- **BKY-PUB-2** (Maven Central + Portal 0.0.2, session 143) — ✅
- **BKY-COV-2** (microfissures DeployProfileTask/ProfilePublisher/BakeryPlugin/ToolsKt) — ✅
- **BKY-I18N** (10 langues, 23/23 pts) — ✅
- **BKY-I18N-MIG** (`migrateToI18n`, 16/16 pts) — ✅
- **BKY-I18N-PROD-MIG / -VAL / -GEN** (migration réelle magic-stick, cccp.education, cheroliv.com) — ✅
- **BKY-I18N-DELTA** (Loi de l'Économie d'Encre, 3/3 pts) — ✅
- **BKY-CR-V5** (60 code smells) — ✅
- **BKY-CR-V6** (3 duplications structurelles, 8/8 pts) — ✅
- **BKY-A11Y-1** (accessibilité WCAG/RGAA, 13/13 pts) — ✅
- **BKY-FIX-1** (deployProfile sans site.yml) — ✅

## Docs d'architecture

- [AGENT.adoc](../bakery-plugin/AGENT.adoc) — Règles absolues & gouvernance
- [INDEX.adoc](../bakery-plugin/.agents/INDEX.adoc) — Sessions, EPICs, roadmap
- [BACKLOG.adoc](../bakery-plugin/BACKLOG.adoc) — Travail restant
- [PROMPT_REPRISE.adoc](../bakery-plugin/PROMPT_REPRISE.adoc) — Mission de la session en cours
- [BAKERY_ROADMAP_JBake.adoc](../bakery-plugin/.agents/BAKERY_ROADMAP_JBake.adoc) — Roadmap expert JBake
- [CODE_REVIEW.adoc](../bakery-plugin/.agents/CODE_REVIEW.adoc) — Revue de code V2→V6
- [TEST_COVERAGE_ANALYSIS.adoc](../bakery-plugin/.agents/TEST_COVERAGE_ANALYSIS.adoc) — Analyse de couverture
- [LAZY_EAGER_ESSENTIALS.adoc](../bakery-plugin/LAZY_EAGER_ESSENTIALS.adoc) — Pattern Lazy/Eager
- [README.adoc](../README.adoc) — README niveau projet (AsciiDoc)
- [LICENCE](../bakery-plugin/LICENCE) — Apache 2.0

## Contribuer

1. Le build compile : `./gradlew build -x test`
2. Republier localement : `./gradlew -q publishToMavenLocal` (Règle 0, obligatoire)
3. Tests unitaires verts : `./gradlew test`
4. Cucumber vert : `./gradlew cucumberTest` (129/129)
5. Couverture respectée : `./gradlew koverThresholdCheck` (≥ 85 %)
6. Suivre les conventions DDD (value objects, ports/adaptateurs, Arrow `Either`/`Raise`, sans fuites)
7. Ne jamais écraser `site.yml` sans sauvegarde (AGENT.adoc §1b — règle de sûreté)
8. Exécuter la procédure de fermeture en 6 étapes (AGENT.adoc §3) en fin de session

## Licence

Apache License 2.0 — voir [LICENCE](../bakery-plugin/LICENCE).

---

_Partie de l'écosystème CCCP Education — `groupId: education.cccp`._
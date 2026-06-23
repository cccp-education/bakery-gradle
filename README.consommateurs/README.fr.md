<!-- translated from README.md rev 0.0.2 -->
# bakery-gradle — Guide Consommateur

> Plugin Gradle de génération de site statique — JBake + Thymeleaf + Firebase Auth + Analytics + Knowledge Graph + LLM.

[![Maven Central](https://img.shields.io/maven-central/v/education.cccp/bakery-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/education.cccp/bakery-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.bakery.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.bakery)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/bakery-gradle/test.yml?branch=main&label=Tests)](https://github.com/cheroliv/bakery-gradle/actions/workflows/test.yml)
[![Licence](https://img.shields.io/github/license/cheroliv/bakery-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.2` · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.bakery`
- **Build** : `./gradlew build` · **Tests** : `./gradlew check` (JUnit5 + functionalTest + Cucumber + E2E Playwright)
- **Couverture** : ≥ 85 % (Kover `koverThresholdCheck`, intégré à `check`) · **Cucumber** : 129/129 PASS

🌐 Langues : [English](README.md) | **Français**

---

## Ce que ça fait

`bakery-gradle` génère et déploie un site statique via **JBake 2.7.0** (templates Thymeleaf,
contenu AsciiDoc/Markdown). Il intègre Firebase Auth + commentaires, Google Forms, Analytics/Newsletter,
un système de thèmes, un audit d'accessibilité (WCAG/RGAA), une migration i18n vers 10 langues, une
génération assistée par IA d'articles/scaffold/thèmes (LangChain4j + Ollama), et le pattern de contexte
augmenté **LENtille** qui alimente le contexte composite N3 pour runner-gradle. La tâche agrégée
`publishSite` bake et déploie sur `gh-pages` en une seule commande.

Partie de l'écosystème multi-plugins CCCP Education :

```
contenu (AsciiDoc/Markdown) → [bakery-gradle] bake → deploy gh-pages
                                                ↑
                    codebase-gradle (RAG) + graphify-gradle (KG) + LLM (Ollama)
```

## Démarrage rapide

### 1. Appliquer le plugin

```gradle
plugins {
    id("education.cccp.bakery") version "0.0.2"
}
```

### 2. Configurer via DSL ou `site.yml`

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

`site.yml` (non versionné — ne jamais écraser sans sauvegarde) pilote JBake, le déploiement, Firebase,
analytics, le thème, le layout et le contexte augmenté.

### 3. Baker et déployer

```bash
./gradlew publishSite        # agrégat : bake + deploy gh-pages
./gradlew bake               # rendu JBake seul
./gradlew deploySite          # pousser la sortie bakée vers gh-pages
```

## Tâches disponibles

| Tâche | Groupe | Description |
|-------|--------|-------------|
| `bake`                      | —        | Rendu JBake (configuré par `configureBakeTask`) |
| `generateSite`              | generate | Initialise les dossiers site + maquette (type : `blog`/`basic`) |
| `generateSiteFromIntention` | generate | Scaffold de site assisté par IA (interactif, Ollama) |
| `generateTheme`             | generate | Génère un thème depuis le catalogue (variante + surcharges) |
| `generateArticle`           | generate | Article de blog assisté par IA → `content/blog/YYYY/MM/` |
| `migrateToI18n`             | transform | Migre un site bakery existant vers i18n (scanne templates, extrait le texte hardcodé, génère `messages_{lang}.properties`) |
| `pagefind`                  | transform | Indexe le site baké avec Pagefind pour la recherche plein texte |
| `deploySite`                | deploy    | Pousse la sortie bakée vers la branche `gh-pages` GitHub (JGit) |
| `deployMaquette`            | deploy    | Pousse les fichiers maquette vers le dépôt |
| `deployProfile`             | deploy    | Pousse les fichiers de profil (ex. README.md) vers le dépôt GitHub |
| `publishSite`               | publish   | **Agrégat** — bake + deploy `gh-pages` (pratique) |
| `collectSiteConfig`         | collect   | Initialise la configuration Bakery |
| `collectSiteContext`        | collect   | Collecte le contexte du site baké → `build/bakery/metadata.json` pour runner-gradle N3 |
| `collectAugmentedContext`   | collect   | Collecte le contexte augmenté LENS (ségrégation + enrichissement + budget) → `build/bakery/augmented-context.json` |
| `serve`                     | info      | Sert le site baké localement (JavaExec) |
| `validateFirebaseConfig`   | validate  | Valide la cohérence de la configuration Firebase (mécanique + IA optionnelle) |
| `verifyConfigurationMapping`| verification | Valide le mapping YAML `site.yml` → `SiteConfiguration` et masque les secrets |
| `accessibilityAudit`        | audit     | Audit d'accessibilité WCAG/RGAA du site baké — rapport JSON/ASCII |
| `installPlaywright`          | verification | Installe le navigateur Chromium Playwright pour les tests E2E |

> **Mode dégradé** : quand `configPath` est absent/invalide, le plugin bascule en mode
> **scaffold-only** (`generateSite`, `generateSiteFromIntention`, `deployProfile`). Définissez
> `configPath` via le DSL, `gradle.properties` (`bakery.config.path=site.yml`), ou `-Pbakery.config.path=...`.

## DSL d'extension

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
    theme         { /* BKY-JB-6 vars CSS, dark/light, logo */ }
    layout        { /* BKY-JB-7 FULL_WIDTH, SIDEBAR_LEFT, SIDEBAR_RIGHT, CENTERED */ }
    articleIntention  { /* BKY-JB-8 topic, ton, audience, keywords, lang */ }
    augmentedContext  { /* BKY-LENS budget + injection */ }
    scaffoldIntention { /* BKY-IA-1 description, siteType, lang, projectName */ }
    themeIntention    { /* BKY-IA-2 description, variante, surcharges */ }
    i18nMigration     { /* BKY-I18N-MIG siteDir, languages, defaultLanguage, dryRun */ }
    a11y              { /* BKY-A11Y-1 auditDir, reportPath, conformanceLevel (AA/AAA), failOnNonCompliant */ }
}
```

## Prérequis

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5.1+
- **Node.js** (pour Pagefind + Playwright via le plugin Gradle Node `7.1.0`)
- **Ollama** sur le port `11437–11465` (ports `11434–11436` interdits) — modèles `gpt-oss:120b-cloud`, `gemma4:31b-cloud`
- **GPG** (uniquement pour la publication sur Maven Central)

## Build et tests

```bash
./gradlew build                     # build complet (UT + functionalTest + Cucumber)
./gradlew test                      # tests unitaires JUnit5 (exclut le moteur Cucumber)
./gradlew functionalTest            # tests fonctionnels GradleTestKit
./gradlew cucumberTest              # Cucumber BDD (129/129 PASS)
./gradlew e2eTest                   # E2E Playwright (installe Chromium)
./gradlew check                      # functionalTest + cucumberTest + koverThresholdCheck
./gradlew koverThresholdCheck       # couverture ≥ 85 %
./gradlew publishToMavenLocal       # publier localement
./gradlew publishAggregationToCentralPortal --no-daemon   # Maven Central
```

## Dépannage

| Symptôme | Solution |
|----------|----------|
| `configPath is not set`          | Définir `bakery { configPath = "site.yml" }`, `gradle.properties`, ou `-Pbakery.config.path=...` |
| `Failed to read site.yml`        | Vérifier que le fichier existe et est un YAML valide ; le plugin bascule en scaffold-only |
| `Java heap space`                | `export GRADLE_OPTS="-Xmx2g"` (cucumberTest maxHeap 1g) |
| Chromium Playwright manquant      | `./gradlew installPlaywright` (auto-lancé par `e2eTest`) |
| Connexion Ollama refusée          | Vérifier le port dans `11437–11465` ; `11434–11436` interdits |
| Push gh-pages refusé              | Vérifier la clé SSH JGit + la config `pushPage` dans `site.yml` |

Voir [AGENT.adoc](../bakery-plugin/AGENT.adoc) pour la gouvernance et [README.adoc](../README.adoc) pour le contexte projet.

## Licence

Apache License 2.0 — voir [LICENCE](../bakery-plugin/LICENCE).

---

_Partie de l'écosystème CCCP Education — `groupId: education.cccp`._
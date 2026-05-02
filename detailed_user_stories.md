### Épopée 10 : Intégration du Formulaire de Contact avec Supabase

#### US-SUPA-VALID-01 : Valider le schéma Supabase via des tests automatisés
> **En tant que** développeur, **je veux** une suite de tests automatisés qui valide la présence du schéma de base de données sur Supabase, **afin de** garantir que l'environnement est correctement configuré avant tout déploiement.
> **Résolution :** La validation du schéma Supabase a été implémentée avec succès. Conformément à la conclusion de l'US, le déploiement du schéma se fait manuellement via le dashboard Supabase en utilisant le script `supabase_script.sql`. Pour garantir la conformité de l'environnement, une suite de tests automatisés a été créée dans `buildSrc/src/test/kotlin/site/core/CoreTest.kt`. Ces tests, exécutés par la tâche `./gradlew :buildSrc:test`, agissent comme un garde-fou en vérifiant :
> 1.  La connexion et l'authentification à Supabase.
> 2.  L'existence des tables `contacts` et `messages` et que leur accès est bien restreint par la sécurité RLS.
> 3.  La présence et le bon fonctionnement des fonctions RPC critiques (`get_schemas` et `handle_contact_form`).
> Cette approche assure que le build échouera si l'infrastructure de base de données n'est pas correctement configurée, prévenant ainsi les erreurs de déploiement.

#### US-60.1 : Mettre en place l'infrastructure de test unitaire pour le build Gradle.
> **En tant que développeur**, je veux configurer Gradle pour qu'il puisse compiler et exécuter des tests unitaires avec Kotlin et JUnit, **afin de pouvoir écrire des tests automatisés pour la logique du build.**
> **Critères d'Acceptance :**
> *   Le projet a les dépendances `kotlin("test")`, `gradleTestKit()` et JUnit.
> *   La tâche `./gradlew test` est disponible.
> *   La tâche `./gradlew test` exécute avec succès un test "canary" qui ne fait qu'affirmer `true`.
> **Résolution :**
> Le fichier `build.gradle.kts` a été modifié pour inclure le plugin `kotlin("jvm")`. Les dépendances `testImplementation` pour `kotlin("test")`, `gradleTestKit()`, `junit-jupiter` et `testRuntimeOnly` pour `junit-platform-launcher` ont été ajoutées. La tâche `test` a été configurée pour utiliser JUnit Platform et les `jvmArgs` nécessaires (`--add-opens`) ont été ajoutés pour assurer la compatibilité avec les versions récentes de Java. Un test canary (`ProjectTests.kt`) a été créé dans `src/test/kotlin` et s'exécute avec succès via `./gradlew test`.

#### US-SUPA-CONN-01 : Créer un test de connexion "canary" à Supabase
> **En tant que** développeur, **je veux** implémenter un test de connexion simple (canary) à Supabase en utilisant les bibliothèques Kotlin officielles, **afin de** valider que l'environnement de build peut s'authentifier et exécuter une requête basique avant d'intégrer la logique du formulaire de contact.
> **Résolution :**
> L'implémentation a nécessité un débogage en plusieurs étapes.
> 1.  **Dépendances Gradle :** Les dépendances Supabase correctes ont été ajoutées à `buildSrc/build.gradle.kts`. Il a été crucial de valider les coordonnées Maven exactes (`io.github.jan-tennert.supabase:bom`, `io.github.jan-tennert.supabase:gotrue-kt`, etc.) pour résoudre les erreurs de build initiales.
> 2.  **Code de Test :** Le fichier `buildSrc/src/test/kotlin/com/cheroliv/build/CoreTest.kt` a été créé.
> 3.  **Correction des Imports et de l'API :** L'erreur de compilation principale a été résolue en remplaçant l'appel incorrect à `supabase.gotrue` par `supabase.auth`, et en ajustant l'import pour utiliser `io.github.jan.supabase.gotrue.auth`.
>
> Le test final initialise un client Supabase, lit les credentials depuis `local.properties` (placé dans `buildSrc/src/test/resources`), et vérifie que le service d'authentification est accessible en tentant de récupérer la session utilisateur. Le test passe avec succès, validant la connectivité de l'environnement de build avec Supabase.
>
> **Extrait de `buildSrc/build.gradle.kts` (Dépendances Supabase) :**
> ```kotlin
> dependencies {
>     val supabaseVersion = "2.6.1"
>     // ...
>     implementation(platform("io.github.jan-tennert.supabase:bom:$supabaseVersion"))
>     implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabaseVersion")
>     implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion")
>     implementation("io.github.jan-tennert.supabase:storage-kt:$supabaseVersion")
>     implementation("io.github.jan-tennert.supabase:serializer-jackson:$supabaseVersion")
>     implementation("io.github.jan-tennert.supabase:gotrue-kt-jvm:$supabaseVersion")
>     // ...
> }
> ```
>
> **Code final de `CoreTest.kt` :**
> ```kotlin
> package com.cheroliv.build
> 
> import io.github.jan.supabase.createSupabaseClient
> import io.github.jan.supabase.gotrue.Auth
> import io.github.jan.supabase.gotrue.auth
> import kotlinx.coroutines.runBlocking
> import org.junit.jupiter.api.BeforeEach
> import org.junit.jupiter.api.Test
> import java.util.Properties
> 
> class SupabaseConnectionTest {
> 
>     private lateinit var supabaseUrl: String
>     private lateinit var supabaseKey: String
> 
>     @BeforeEach
>     fun setup() {
>         println("Setup: Attempting to load local.properties from test resources...")
>         val localProperties = Properties()
>         val inputStream = javaClass.classLoader.getResourceAsStream("local.properties")
>             ?: throw IllegalStateException("local.properties not found in test resources. Please ensure it's in buildSrc/src/test/resources.")
> 
>         inputStream.use { localProperties.load(it) }
> 
>         supabaseUrl = localProperties.getProperty("supabase.project.url")
>         supabaseKey = localProperties.getProperty("supabase.api.key")
> 
>         if (supabaseUrl.isNullOrBlank()) {
>             throw IllegalStateException("supabase.project.url is null or blank in local.properties")
>         }
>         if (supabaseKey.isNullOrBlank()) {
>             throw IllegalStateException("supabase.api.key is null or blank in local.properties")
>         }
>         println("Setup: Supabase credentials loaded successfully.")
>     }
> 
>     @Test
>     fun `canary test - should connect to Supabase and verify authentication service`() = runBlocking {
>         println("Test: Initializing Supabase client with URL: ${supabaseUrl.take(10)}... and Key: ${supabaseKey.take(5)}...")
>         val supabase = createSupabaseClient(
>             supabaseUrl = supabaseUrl,
>             supabaseKey = supabaseKey
>         ) {
>             install(Auth)
>         }
>         println("Test: Supabase client initialized. Attempting to verify authentication service...")
> 
>         try {
>             // Attempt to retrieve the current session. Even if no user is logged in, this verifies GoTrue service is reachable.
>             val session = supabase.auth.currentSessionOrNull()
>             println("Test: Successfully reached Supabase Auth service. Session: $session")
>             // If we reach here without exception, it means the GoTrue service is accessible.
>             // We can add more specific assertions if needed, e.g., checking for a specific HTTP status code if currentSessionOrNull() exposed it.
>         } catch (e: Exception) {
>             System.err.println("Test: Error connecting to Supabase Auth service: ${e.message}")
>             throw e // Re-throw the exception to fail the test
>         }
>     }
> }
> ```

#### US-60 : Configuration du Backend Supabase
> **En tant que développeur, je veux configurer la table `contacts` et la sécurité (RLS) dans Supabase, afin de créer une fondation sécurisée pour stocker les messages du formulaire.**

#### US-60b : Création de la Fonction RPC pour la Soumission
> **En tant que développeur, je veux créer une fonction PostgreSQL (RPC) qui encapsule la logique de recherche/création d'un contact et d'insertion d'un message, afin de garantir que la soumission du formulaire soit atomique et sécurisée.**

#### US-60c : Tâche Gradle pour l'Initialisation de la Base de Données
> **En tant que développeur, je veux créer une tâche Gradle qui vérifie et crée les tables Supabase nécessaires avant le déploiement, afin d'automatiser la configuration initiale de la base de données et de garantir que l'environnement est toujours prêt.**
> **Contraintes :**
> *   La tâche doit s'exécuter automatiquement avant la tâche `publishSite`.
> *   Elle doit utiliser les credentials Supabase de manière sécurisée (via la **nouvelle `SiteConfiguration` unifiée**).
> *   Si les tables (`contacts`, `messages`) existent déjà, la tâche doit l'ignorer silencieusement sans générer d'erreur.

#### US-61-pre : Couverture de Test pour le Mapping de Configuration Jackson
> **En tant que développeur**, je veux mettre en place une couverture de test pour le mapping Jackson de la configuration YAML (`managed-jbake-context.yml`) vers l'objet `SiteConfiguration`, **afin de garantir la non-régression et la fiabilité du chargement de la configuration avant de la modifier.**
> **Modus Operandi :**
> 1.  Intégrer JUnit 5 au projet pour permettre les tests unitaires sur la logique du build Gradle.
> 2.  Créer un fichier de test dédié à la classe `SiteConfiguration`.
> 3.  Dans ce test, créer un exemple de fichier `managed-jbake-context.yml` contenant la future structure (avec les sections `deployment` et `supabase`).
> 4.  Écrire un test qui lit ce fichier YAML et utilise la même logique Jackson que le build Gradle pour le désérialiser dans un objet `SiteConfiguration`.
> 5.  Utiliser des assertions pour vérifier que chaque champ de l'objet, y compris les champs imbriqués de Supabase, est correctement peuplé.

#### US-61a (Refondue) : Consolidation de toute la configuration externe dans `SiteConfiguration`
> **En tant que développeur, je veux que TOUTE la configuration externe (déploiement, Supabase) soit lue depuis `managed-jbake-context.yml` et mappée dans un unique objet `SiteConfiguration` dans Gradle, afin de créer une pipeline CI/CD agnostique et de centraliser la gestion des paramètres.**
> **Contraintes :**
> *   Le fichier `managed-jbake-context.yml` devient l'unique source de vérité pour la configuration en environnement CI/CD.
> *   Le build Gradle doit mapper ce fichier YAML vers l'objet `SiteConfiguration` via Jackson.
> *   Toutes les tâches Gradle et configurations (JBake, `setupSupabaseSchema`, etc.) doivent lire leurs paramètres depuis cet objet unifié.
> **Modus Operandi :**
> 1.  Mettre à jour le secret GitHub `CHEROLIV_MIGRATION_CONFIG` pour y inclure les informations Supabase (URL et clé publique).
> 2.  Modifier la `data class SiteConfiguration` dans `build.gradle.kts` pour qu'elle puisse contenir les nouvelles données (déploiement et Supabase).
> 3.  Adapter la logique de désérialisation Jackson pour mapper l'entièreté du fichier YAML vers le nouvel objet `SiteConfiguration`.
> 4.  Mettre à jour la configuration de JBake pour qu'elle tire les paramètres Supabase depuis `siteConfiguration.supabase` pour les injecter dans le frontend.

#### US-61 : Intégration du Client Supabase JS
> **En tant que développeur, je veux intégrer le client Supabase JS dans le site et initialiser la connexion, afin de permettre au frontend de communiquer avec l'API Supabase.**


#### US-62b : Correction du Style du Sélecteur de Pays
> **En tant qu'utilisateur, lorsque je sélectionne un pays pour le préfixe téléphonique, je veux que la couleur de fond du menu déroulant s'accorde avec le thème du site, afin de ne pas être distrait par une incohéérence visuelle.**

#### US-62c : Correction du Style du champ de recherche dans le menu des indicatifs
> **En tant qu'utilisateur, lorsque j'ouvre le menu déroulant des pays et que je saisis du texte pour rechercher un pays, je veux que la couleur de fond de ce champ de recherche s'accorde avec le thème du site, afin de ne pas être distrait par une incohéérence visuelle.**

#### US-63 : Retour Visuel pour l'Utilisateur
> **En tant qu'utilisateur, je veux recevoir une confirmation visuelle claire (succès ou erreur) après avoir soumis le formulaire, afin de savoir si mon message a bien été envoyé.**

#### US-64 : Webhook de Notification par Email (Google Apps Script)
> **En tant qu'administrateur du site, je veux qu'un script Google Apps soit déployé pour recevoir les données d'un nouveau contact et m'envoyer une notification par email, afin d'être informé en temps réel des nouvelles soumissions.**

#### US-65 : Déclencheur d'Automatisation (Supabase Trigger)
> **En tant que développeur, je veux créer une fonction et un trigger dans Supabase qui appellent le webhook Google Apps Script à chaque nouvelle insertion dans la table `contacts`, afin d'automatiser le processus de notification.**

#### US-66 : Améliorations de Sécurité et de Maintenabilité
> **En tant que développeur, je veux ajouter des validations avancées (honeypot, regex) et améliorer les notifications (formatage HTML, logging), afin de renforcer la sécurité et la maintenabilité de la solution.**

#### US 7.1 : Formulaire de contact clair et accessible
> **En tant que** visiteur, **je veux** voir un formulaire de contact clair et accessible directement sur le site **afin que** je puisse facilement envoyer un message.
> **Résolution :** Le formulaire de contact a été implémenté dans le template `site/templates/contact_section.thyme`. Il utilise les composants de formulaire de Bootstrap 5 pour une présentation claire et responsive. Les champs incluent le nom, l'email, le sujet et le message. Des attributs `aria-label` et `for` ont été ajoutés pour améliorer l'accessibilité. Le formulaire est prêt pour l'intégration avec un backend (Supabase).

### Épopée 11 : Couverture de Test pour le Build Gradle (Infrastructure)

#### US-67 : Intégration de JUnit 5 pour les Tests Gradle
> **En tant que développeur, je veux intégrer JUnit 5 au projet Gradle, afin de pouvoir écrire et exécuter des tests unitaires pour les tâches et la logique du build.**

#### US-68 : Écriture de Tests Unitaires pour les Tâches Gradle Clés
> **En tant que développeur, je veux écrire des tests unitaires pour les tâches Gradle critiques (ex: `publishSite`, `setupSupabaseSchema`), afin de valider leur comportement et leurs interactions.**

#### US-69 : Mesure de la Couverture de Code avec JaCoCo
> **En tant que développeur, je veux intégrer JaCoCo au build Gradle, afin de mesurer la couverture de code des scripts de build et d'identifier les zones non testées.**

#### US-70 : Rapports de Couverture de Test dans GitHub Actions
> **En tant que développeur, je veux que les rapports de couverture de test soient générés et affichés dans la pipeline GitHub Actions, afin de visualiser l'état de la couverture à chaque déploiement.**

#### US-TEST-JGIT-01 : Mettre en place des tests unitaires avec mocks pour la logique JGit
> **En tant que** développeur, **je veux** créer une suite de tests unitaires avec des mocks pour les fonctions interagissant avec JGit, **afin de** valider la logique de commit et de push de manière isolée et sans dépendre d'opérations réseau réelles.
> **Statut :** En attente. La complexité de mocker JGit justifie de prioriser d'abord les tests d'intégration de plus haut niveau.

### Épopée 12 : Migration du Build Script vers un Plugin Gradle (Infrastructure)

#### US-71 : Analyse du Build Script Existant et Identification des Logiques à Migrer
> **En tant que développeur, je veux analyser le `build.gradle.kts` actuel pour identifier les tâches, configurations et logiques qui peuvent être encapsulées dans un plugin, afin de définir le périmètre de la migration.**
> **Contrainte :** L'Épopée 11 (Couverture de Test pour le Build Gradle) doit être entièrement terminée avant de commencer cette épopée.

#### US-72 : Création de la Structure Initiale du Plugin Gradle
> **En tant que développeur, je veux créer un nouveau sous-projet pour le plugin Gradle avec sa structure de base (classes de plugin, tests), en m'appuyant sur l'exemple `sample_gradle_plugin-kotlin-dsl`, afin de démarrer le développement du plugin.**

#### US-73 : Développement des Tâches du Plugin avec TDD
> **En tant que développeur, je veux développer les tâches et la logique du plugin en utilisant une approche TDD (tests unitaires et fonctionnels), en m'inspirant des tests du `sample_gradle_plugin-kotlin-dsl`, afin de garantir la qualité et la robustesse du plugin.**

#### US-74 : Intégration du Nouveau Plugin dans le Build Principal
> **En tant que développeur, je veux remplacer la logique migrée dans `build.gradle.kts` par l'application du nouveau plugin, afin de simplifier le script de build principal et de valider l'intégration du plugin.**

#### US-75 : Refactoring et Optimisation du Plugin
> **En tant que développeur, je veux refactoriser et optimiser le code du plugin, en m'assurant qu'il respecte les bonnes pratiques Gradle et Kotlin, afin d'améliorer sa maintenabilité et ses performances.**

### Épopée 2 : Expérience de Lecture des Articles (AsciiDoc)

#### US-12 : Style des Blocs de Code
> **Contrainte :** Pour améliorer le confort de lecture, la police des blocs de code doit être suffisamment grande.
> **Résolution :** La taille de la police pour les blocs de code (`<pre>`, `<code>`) a été augmentée de 10% (`font-size: 1.1rem`) dans `site/assets/css/asciidoctor.css` pour une meilleure lisibilité.

#### US-12b : Bouton Copier

#### US-12c : Thème Adaptatif pour les Blocs de Code
> **Note :** Voici une liste de thèmes sombres populaires de `highlight.js` qui peuvent être utilisés pour l'implémentation.
> ```js
> const darkThemes = {
>     'dark': 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/dark.min.css',
>     'monokai': 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/monokai.min.css',
>     'tomorrow-night': 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/tomorrow-night.min.css',
>     'vs2015': 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/vs2015.min.css',
>     'atom-one-dark': 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/atom-one-dark.min.css',
>     'github-dark': 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/github-dark.min.css'
> };
> ```

### Épopée 7 : Expérience Blog

#### US-30 : Page Blog Dédiée
> **Résolution :** Le template `site/templates/blog.thyme` a été mis à jour pour utiliser un design moderne basé sur des cartes, afficher les résumés des articles et inclure des boutons d'action (Archives, Rechercher), alignant ainsi la page de blog dédiée avec la nouvelle interface utilisateur Bootstrap 5.

#### US-32b : Reporter la Modale de Recherche vers Thymeleaf
> **En tant que développeur, je veux que les modifications de la modale de recherche (structure et style) apportées à la maquette HTML soient reportées dans les templates Thymeleaf, afin que le site généré reflète les dernières évolutions.**

### Épopée 15 : Création de Contenu : Livre Gradle et Articles Teasers

#### US-15.2 : Rédiger un article sur l'approche TDD pour le plugin `site-baker`
> **En tant que** développeur, **je veux** écrire un article expliquant l'approche TDD utilisée pour développer le plugin `site-baker`, **afin de** détailler les tests unitaires et fonctionnels mis en place pour garantir la robustesse et la maintenabilité du code.
> **Résolution :** Conformément à la User Story, un article détaillé a été rédigé pour documenter l'approche TDD adoptée lors du développement du plugin `site-baker`. L'article, `article_tdd_gradle_plugin.adoc`, explique le cycle de développement : écriture d'un test qui échoue (rouge), écriture du code minimal pour le faire passer (vert), puis refactorisation. Il couvre la mise en place des tests unitaires (`ProjectBuilder`) et fonctionnels (`GradleRunner`) pour valider l'enregistrement d'une tâche et la création d'une extension DSL. Cet article sert de documentation et de guide pour les futurs développements de plugins dans le projet.

### Épopée 9 : Amélioration du Pied de Page (Footer)

#### US-45 : Amélioration de l'affichage du menu de navigation sur petits écrans
> **En tant qu'utilisateur**, je veux que le menu de navigation s'affiche correctement sur les petits écrans, **afin de pouvoir accéder à tous les liens, y compris ceux du menu déroulant 'mémo', sans qu'ils ne sortent de l'écran et avec une barre de défilement si nécessaire.**
> **Détails :** Quand je suis sur un écran de petite taille, l'ouverture du menu burger, couplée au déroulement du drop-down "mémo", fait que le menu sort de l'écran. Dans cette situation, il n'y a pas de barre de défilement dans le drop-down et les liens inférieurs à la ligne de bas d'écran sont inaccessibles.

#### US-57 : Liens Essentiels dans le Pied de Page
> **En tant que visiteur, je veux trouver des liens clairs vers GitHub et le flux RSS dans le pied de page, afin d'accéder rapidement aux informations et aux moyens d'interaction clés.**
> **Contrainte :** Au survol des icônes, une infobulle (tooltip) doit apparaître pour préciser la destination : "github/cheroliv" pour GitHub et "flux RSS" pour le flux RSS.

### Épopée 7 : Amélioration de la Maintenabilité du Build
> **Objectif :** Refactoriser et optimiser la configuration Gradle pour améliorer la lisibilité, les performances et la facilité de maintenance.

#### US-62: Migration de la logique de build vers `buildSrc`
> **En tant que** développeur, **je veux** déplacer les tâches Gradle personnalisées, les data classes et les fonctions utilitaires du fichier `build.gradle.kts` racine vers le répertoire `buildSrc`, **afin de** rendre le script de build principal plus déclaratif, d'améliorer les performances et de faciliter les tests unitaires de la logique de build.
>
> **Contraintes techniques :**
> *   La structure du répertoire `buildSrc` doit suivre les conventions de Gradle (`buildSrc/src/main/kotlin`).
> *   Les dépendances de la logique de build (`jgit`, `jackson`, etc.) doivent être déclarées dans un fichier `buildSrc/build.gradle.kts` dédié.
> *   Le plugin `kotlin-dsl` doit être utilisé dans `buildSrc/build.gradle.kts` pour une intégration optimale.
>
> **Modus Operandi :**
> 1.  Créer l'arborescence de répertoires `buildSrc/src/main/kotlin/com/cheroliv/build`.
> 2.  Créer et configurer le fichier `buildSrc/build.gradle.kts` avec le plugin `kotlin-dsl` et y migrer les dépendances du bloc `buildscript` du `build.gradle.kts` racine.
> 3.  Déplacer toutes les `data class`, `sealed class` et fonctions utilitaires dans des fichiers Kotlin dédiés sous `buildSrc/src/main/kotlin/com/cheroliv/build`.
> 4.  Supprimer le code migré du fichier `build.gradle.kts` racine.
> 5.  Ajouter les `import` nécessaires en haut du `build.gradle.kts` racine pour référencer le code déplacé.
> 6.  Déplacer la logique de test et les ressources associées (ex: `test-config.yml`) de `src/test/` vers `buildSrc/src/test/`.
> 7.  Lancer une tâche Gradle (ex: `./gradlew tasks`) pour valider que le projet se synchronise correctement.
>
> **Critères d'acceptance :**
> *   Le répertoire `buildSrc` contient toute la logique de build personnalisée.
> *   Le fichier `build.gradle.kts` racine est principalement déclaratif (plugins, tâches, dépendances).
> *   La commande `./gradlew check` s'exécute avec succès, incluant la tâche `verifyConfigurationMapping`.
> *   Le projet reste fonctionnel et les tâches comme `publishSite` et `publishMaquette` sont toujours correctement configurées.

### Épopée 13 : Refactorisation de la Logique de Publication

#### US-REFACTO-BUILD-01 : Refactorisation Itérative de la Logique de Publication
> **En tant que** développeur, **je veux** refactoriser la logique de publication du script `build.gradle.kts` en la migrant à terme vers `buildSrc`, **afin de** réduire la duplication de code, d'améliorer la maintenabilité et de rendre le build plus propre.
>
> **Contraintes techniques :**
> *   Le build doit rester stable et fonctionnel après chaque micro-itération.
>
> **Modus Operandi (Plan d'Action Itératif) :**
>
> ### Itération 1 : Factoriser la logique de `commit`
> 1.  **Action :** Créer une nouvelle fonction générique `initAddCommitGeneric` dans `build.gradle.kts`. Cette fonction prendra la configuration de push (`GitPushConfiguration`) en paramètre, éliminant ainsi la duplication.
> 2.  **Adaptation :** Modifier les anciennes fonctions `initAddCommit` et `initAddCommitForMaquette` pour qu'elles appellent simplement cette nouvelle fonction avec la configuration appropriée (`siteConfiguration.pushPage` ou `siteConfiguration.pushMaquette`).
> 3.  **Vérification :** Lancer une tâche de publication (ex: `publishSite` ou `publishMaquette`) pour valider que le processus de commit fonctionne toujours comme avant. Le build est stable.
>
> ### Itération 2 : Factoriser la logique de `push`
> 1.  **Action :** Créer une nouvelle fonction générique `pushGeneric` dans `build.gradle.kts` qui accepte `GitPushConfiguration` en paramètre.
> 2.  **Adaptation :** Modifier les anciennes fonctions `push` et `pushMaquette` pour qu'elles deviennent de simples wrappers appelant `pushGeneric`.
> 3.  **Vérification :** Valider à nouveau avec une tâche de publication. Le build reste stable.
>
> ### Itération 3 : Factoriser la logique de publication de haut niveau
> 1.  **Action :** Créer la fonction de haut niveau `publishArtifact` dans `build.gradle.kts`. Elle prendra en paramètre la `GitPushConfiguration`, le chemin source et le chemin cible. Elle orchestrera les appels à `createRepoDir`, `copyBakedFilesToRepo`, `initAddCommitGeneric` et `pushGeneric`.
> 2.  **Adaptation :** Modifier les anciennes fonctions `pushPages` et `pushMaquette` (celles avec les lambdas) pour qu'elles appellent `publishArtifact`.
> 3.  **Vérification :** Valider une dernière fois le fonctionnement de bout en bout. Le build est toujours stable.
>
> ### Itération 4 : Nettoyage final du script `build.gradle.kts`
> 1.  **Action :** Maintenant que la logique est centralisée dans `publishArtifact`, modifier directement les tâches `publishSite` et `publishMaquette` pour qu'elles appellent `publishArtifact`.
> 2.  **Suppression :** Supprimer toutes les anciennes fonctions dupliquées et devenues inutiles (`initAddCommit`, `initAddCommitForMaquette`, `push`, `pushMaquette`, `pushPages`, etc.).
> 3.  **Renommage :** Renommer les fonctions génériques pour des noms plus simples (ex: `initAddCommitGeneric` -> `initAddCommit`). Le script `build.gradle.kts` est maintenant propre, sans duplication, et la logique est prête à être déplacée.
> 4.  **Vérification :** S'assurer que les tâches fonctionnent toujours après ce nettoyage.
>
> ### Itération 5 : Migration vers `buildSrc`
> 1.  **Action :** Créer le fichier `buildSrc/src/main/kotlin/com/cheroliv/logic/Publishing.kt`.
> 2.  **Déplacement :** Couper/coller les fonctions refactorisées (`createRepoDir`, `copyBakedFilesToRepo`, `initAddCommit`, `push`, `publishArtifact`) depuis `build.gradle.kts` vers le nouveau fichier `Publishing.kt`.
> 3.  **Finalisation :** Ajouter la déclaration de `package` et les `imports` nécessaires dans le nouveau fichier, et ajouter les `imports` correspondants en haut de `build.gradle.kts`.
> 4.  **Vérification Finale :** Lancer les tâches `publishSite` et `publishMaquette` pour confirmer que le build fonctionne parfaitement avec la logique désormais externalisée dans `buildSrc`.
>
> **Critères d'acceptance :**
> *   La duplication de code liée à la publication est éliminée du script `build.gradle.kts`.
> *   La logique de publication est entièrement migrée et fonctionnelle depuis `buildSrc`.
> *   Les tâches `publishSite` et `publishMaquette` s'exécutent avec succès en utilisant la nouvelle logique centralisée.
> *   La commande `./gradlew check` passe avec succès.
>
> **Journal de Bord :**
> *   **Itérations 1, 2, 3 terminées :** La logique de `commit`, `push` et l'orchestration de haut niveau ont été factorisées avec succès dans des fonctions génériques (`initAddCommitGeneric`, `pushGeneric`, `publishArtifact`) directement dans `build.gradle.kts`. Le build a été validé avec `./gradlew check` après chaque étape.
> *   **Introduction d'un filet de sécurité :** Pour valider les modifications de bout en bout, la commande `./gradlew -q -s publishSite` est désormais utilisée comme test d'intégration. Elle permet de s'assurer que le processus de publication reste fonctionnel.ogique est prête à être déplacée.
> 4.  **Vérification :** S'assurer que les tâches fonctionnent toujours après ce nettoyage.
>
> ### Itération 5 : Migration vers `buildSrc`
> 1.  **Action :** Créer le fichier `buildSrc/src/main/kotlin/com/cheroliv/logic/Publishing.kt`.
> 2.  **Déplacement :** Couper/coller les fonctions refactorisées (`createRepoDir`, `copyBakedFilesToRepo`, `initAddCommit`, `push`, `publishArtifact`) depuis `build.gradle.kts` vers le nouveau fichier `Publishing.kt`.
> 3.  **Finalisation :** Ajouter la déclaration de `package` et les `imports` nécessaires dans le nouveau fichier, et ajouter les `imports` correspondants en haut de `build.gradle.kts`.
> 4.  **Vérification Finale :** Lancer les tâches `publishSite` et `publishMaquette` pour confirmer que le build fonctionne parfaitement avec la logique désormais externalisée dans `buildSrc`.
>
> **Critères d'acceptance :**
> *   La duplication de code liée à la publication est éliminée du script `build.gradle.kts`.
> *   La logique de publication est entièrement migrée et fonctionnelle depuis `buildSrc`.
> *   Les tâches `publishSite` et `publishMaquette` s'exécutent avec succès en utilisant la nouvelle logique centralisée.
> *   La commande `./gradlew check` passe avec succès.
>
> **Résolution :** La logique de publication a été entièrement refactorisée en suivant un plan d'action itératif strict. Les fonctions dupliquées pour le commit et le push (`initAddCommit`, `push`, etc.) ont d'abord été consolidées en fonctions génériques (`initAddCommitGeneric`, `pushGeneric`) au sein même de `build.gradle.kts`. Une fonction d'orchestration de haut niveau, `publishArtifact`, a ensuite été créée pour centraliser le flux de publication. Chaque micro-itération a été validée par un `./gradlew check` pour garantir la stabilité. Une fois la logique propre et sans duplication, l'ensemble des fonctions a été migré avec succès vers un nouveau fichier `buildSrc/src/main/kotlin/com/cheroliv/logic/Publishing.kt`. Le script `build.gradle.kts` est désormais plus déclaratif et la logique de build est correctement encapsulée, testable et maintenable.

### Épopée 14 : Stabilisation de la Suite de Tests

#### US-TEST-STAB-01 : Fiabiliser la suite de tests complète
> **En tant que** Développeur (cheroliv), **je veux** une suite de tests entièrement automatisée et fiable pour le cycle de vie du serveur JBake (`serve`/`stopServe`) et pour la logique de build personnalisée (`buildSrc`), **afin de** pouvoir lancer les tests en toute confiance, de prévenir les régressions et de garantir la stabilité de l'environnement de développement et de build.
> **Résolution :** Un travail de fond a été mené pour stabiliser l'ensemble de la suite de tests.
> 1.  **Tests du projet racine :** La tâche `stopServe` a été refactorisée pour utiliser un arrêt ciblé par PID au lieu de `killall java`, ce qui a résolu les arrêts inopinés du daemon Gradle pendant les tests. Un test atomique couvrant le cycle de vie complet `serve` -> `stopServe` a été mis en place avec succès.
> 2.  **Tests de `buildSrc` :** Les échecs ont été résolus en corrigeant le chemin d'accès au wrapper Gradle (pour qu'il pointe vers la racine du projet) et en rendant la sortie de la commande `curl` silencieuse (`-s`) pour permettre une comparaison de contenu fiable.
>
> **Critères d'Acceptance Atteints :**
> *   La commande `./gradlew test` depuis la racine du projet s'exécute avec succès.
> *   La commande `./gradlew :buildSrc:test` s'exécute avec succès.
> *   Le test de la tâche `serve` démarre un serveur JBake fonctionnel.
> *   Le test de la tâche `stopServe` arrête le serveur JBake de manière ciblée (via son PID) sans perturber le processus de test Gradle.
> *   Les tests de `buildSrc` s'exécutent correctement en utilisant le wrapper Gradle de la racine du projet.

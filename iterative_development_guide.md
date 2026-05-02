# Guide de la Boucle Itérative de Développement

Ce document décrit le workflow itératif utilisé pour le développement de ce projet. Il a pour but de structurer la création, le suivi, l'implémentation et l'archivage des Épopées et des User Stories (US), et de fournir un cadre pour la collaboration avec l'assistant IA Gemini-CLI.

## 1. Vue d'ensemble du Workflow

Le cycle de vie d'une fonctionnalité (User Story) suit quatre étapes clés, matérialisées par des fichiers de pilotage spécifiques :

1.  **Création :** Une nouvelle US est identifiée et ajoutée au backlog.
    *   *Fichier concerné :* `product_backlog.md`
2.  **Détaillage :** L'US est enrichie de détails techniques, contraintes et critères d'acceptance.
    *   *Fichier concerné :* `detailed_user_stories.md`
3.  **Implémentation :** L'assistant IA prend en charge la réalisation technique de l'US.
4.  **Archivage :** Une fois l'US terminée et validée, elle est archivée pour conserver un historique propre.
    *   *Fichiers concernés :* `GEMINI_archive.md`, `detailed_user_stories.md` (mise à jour avec la résolution), `product_backlog.md` (nettoyage).

## 2. Les 4 Phases du Cycle de Vie d'une User Story

### Phase 1 : Création de l'US

Toute nouvelle demande commence ici.

*   **Action :** L'utilisateur exprime un besoin sous forme de User Story (titre, description "En tant que...", détails).
*   **Responsabilité de Gemini-CLI :**
    1.  Analyser la demande.
    2.  Formater l'US selon les standards du projet.
    3.  L'ajouter au fichier `product_backlog.md`. Ce fichier ne doit contenir **que** les tâches en cours ou prêtes à être démarrées.

### Phase 2 : Détaillage de l'US

Avant de commencer l'implémentation, l'US est détaillée pour clarifier le besoin.

*   **Action :** L'utilisateur et/ou Gemini-CLI ajoutent des informations cruciales :
    *   **Contraintes techniques :** Limitations à respecter.
    *   **Modus Operandi :** La stratégie d'implémentation envisagée.
    *   **Critères d'acceptance :** Comment valider que l'US est "terminée".
*   **Responsabilité de Gemini-CLI :**
    1.  Ajouter la version détaillée de l'US dans `detailed_user_stories.md` sous la bonne Épopée.

### Phase 3 : Implémentation

C'est la phase de développement actif, menée par micro-itérations pour garantir la stabilité et la traçabilité.

*   **Action :** Gemini-CLI réalise les tâches de code nécessaires (lecture, écriture, modification de fichiers) pour répondre aux exigences de l'US.
*   **Responsabilité de Gemini-CLI :**
    1.  Suivre le "Modus Operandi" défini.
    2.  Valider que tous les critères d'acceptance sont remplis.
    3.  Communiquer clairement sur les actions entreprises.

#### Workflow de Micro-Itération (par US et par étape d'US)

Pour chaque User Story ou sous-étape d'une US, le développement suit une boucle itérative stricte :

1.  **Action de l'utilisateur :** L'utilisateur initie une petite modification ou une étape de refactorisation.
    *   **Exemple de prompt :** "J'ai décommenté la section `plugins { `kotlin-dsl` }` dans `buildSrc/build.gradle.kts`. Documente cette étape et lance la vérification."

2.  **Action de Gemini-CLI :**
    *   **Appliquer le changement :** Effectuer la modification de code demandée (si applicable).
    *   **Documenter l'étape :** Mettre à jour `knowledge_base.md` avec la description de l'étape, l'action effectuée et la vérification associée.
    *   **Lancer la vérification :** Exécuter la commande de vérification (`./gradlew -q -s check --rerun-tasks`) pour s'assurer qu'aucune régression n'a été introduite.
    *   **Rapporter le résultat :** Communiquer le succès ou l'échec de la vérification.
    *   **Exemple de réponse :** "J'ai documenté l'étape X dans `knowledge_base.md`. La tâche `check` s'est exécutée avec succès, sans erreur. Le build est stable."

Ce processus est répété pour chaque petite avancée, garantissant une progression contrôlée et une documentation à jour.

Ce processus de micro-itérations ne s'applique pas seulement à l'ajout de fonctionnalités, mais aussi et surtout à l'amélioration continue de la qualité du code. Chaque itération est une opportunité pour appliquer des principes de refactorisation.

*   **Action :** L'utilisateur ou Gemini-CLI identifie une opportunité d'améliorer la structure du code (clarté, cohésion, couplage).
*   **Responsabilité de Gemini-CLI :**
    1.  Proposer des refactorisations basées sur des principes établis.
    2.  Exécuter chaque étape de la refactorisation comme une micro-itération distincte.
    3.  Lancer la tâche de vérification (`./gradlew check`) après chaque étape pour valider la stabilité.

#### Principes de Refactorisation Assistée par CLI

La collaboration avec un assistant CLI pour la refactorisation impose une discipline rigoureuse pour garantir le succès et la sécurité des opérations.

1.  **Atomicité des Changements :**
    *   **Principe :** Chaque opération de refactorisation doit être la plus petite possible. Ne jamais combiner plusieurs changements conceptuels en une seule étape.
    *   **Exemple :** Si l'objectif est de renommer une méthode et d'extraire une partie de sa logique dans une nouvelle fonction, ces deux actions doivent faire l'objet de **deux itérations distinctes**, chacune suivie d'une validation.
    *   **Pourquoi :** En cas d'erreur, l'origine est immédiatement identifiable. Cela évite les régressions complexes où plusieurs changements s'entremêlent.

2.  **Validation Systématique Impérative :**
    *   **Principe :** Après **chaque** modification, même la plus triviale (un simple renommage), il est obligatoire de lancer la suite de vérification complète du projet via la commande `./gradlew check`.
    *   **Cycle :** Le rythme de travail doit être `Modifier -> Valider -> Répéter`. Aucune nouvelle modification ne doit être entreprise tant que la précédente n'a pas été validée par un `check` réussi.
    *   **Pourquoi :** C'est le filet de sécurité. Cela garantit que la base de code est toujours dans un état fonctionnel et prévient l'effet "boule de neige" où une petite erreur non détectée en entraîne de plus grandes.

#### Principes de Refactorisation Appliqués

1.  **Augmenter la Cohésion :** Regrouper les éléments de code qui ont des responsabilités similaires ou qui travaillent ensemble.
    *   **Exemple (basé sur nos dernières itérations) :**
        *   **Situation initiale :** La fonction `createCnameFile` était définie dans `build.gradle.kts`, séparée de la classe de données `SiteConfiguration` sur laquelle elle opère.
        *   **Action de refactorisation :** La fonction a été déplacée dans le fichier `buildSrc/src/main/kotlin/com/cheroliv/data/Configuration.kt`, aux côtés de la définition de `SiteConfiguration`.
        *   **Bénéfice :** La logique et les données qu'elle manipule sont maintenant centralisées, améliorant la lisibilité et la maintenabilité.

2.  **Diminuer le Couplage :** Rendre les composants plus indépendants les uns des autres en clarifiant leurs dépendances.
    *   **Exemple (basé sur nos dernières itérations) :**
        *   **Situation initiale :** La fonction `createCnameFile` dépendait implicitement du contexte global du script `build.gradle.kts`.
        *   **Action de refactorisation :** La fonction a été transformée en une fonction d'extension (`fun SiteConfiguration.createCnameFile(project: Project)`).
        *   **Bénéfice :** Ses dépendances (l'instance de `SiteConfiguration` et l'objet `Project`) sont maintenant explicites. Le code est plus modulaire, plus facile à tester et le script de build principal est simplifié.

L'application de ces principes via des micro-itérations validées garantit que la base de code non seulement s'enrichit de fonctionnalités, mais gagne aussi en robustesse et en qualité à chaque étape.

### Phase 4 : Archivage de l'US

Une fois l'implémentation validée par l'utilisateur, l'US est retirée du flux de travail actif.

*   **Action :** L'utilisateur donne son feu vert pour archiver l'US.
*   **Responsabilité de Gemini-CLI :**
    1.  **Documenter la résolution :** Ajouter un résumé de la solution apportée à l'US dans `detailed_user_stories.md`.
    2.  **Archiver dans `GEMINI_archive.md` :** Copier l'US et sa résolution dans la section "Terminé" de l'archive. Le format doit être cohérent avec les entrées existantes.
    3.  **Nettoyer le backlog :** Supprimer l'US du fichier `product_backlog.md` pour ne laisser que les tâches actives.

---

## 3. Guidance pour Gemini-CLI : Optimisation des Routines

Pour accélérer le processus, voici des routines à lancer en réponse à des commandes utilisateur spécifiques.

### Routine : "Commence l'implémentation de l'US-XX"

1.  **Lire le backlog :** `read_file('product_backlog.md')` pour identifier l'US.
2.  **Lire les détails :** `read_file('detailed_user_stories.md')` pour comprendre les contraintes et le modus operandi.
3.  **Annoncer le plan :** "D'accord, je commence l'implémentation de l'US-XX. Je vais suivre le plan défini dans `detailed_user_stories.md`."
4.  **Exécuter le plan.**

### Routine : "L'US-XX est résolue, tu peux archiver"

1.  **Confirmer la résolution :** Demander à l'utilisateur de décrire brièvement la solution apportée.
2.  **Lire les fichiers de destination :**
    *   `read_file('detailed_user_stories.md')`
    *   `read_file('GEMINI_archive.md')`
3.  **Mettre à jour `detailed_user_stories.md` :** Utiliser `replace` pour ajouter la section "Résolution" à l'US concernée.
4.  **Mettre à jour `GEMINI_archive.md` :** Utiliser `replace` pour ajouter l'US et sa résolution à la liste des tâches terminées.
5.  **Nettoyer `product_backlog.md` :** Utiliser `write_file` pour réécrire le fichier sans l'US terminée.
6.  **Confirmer la fin :** "L'archivage de l'US-XX est terminé. Le backlog est prêt for la prochaine tâche."

---

## 4. Cas Pratique Avancé : Conception et Industrialisation d'un Service Interne (RAG)

Au-delà de la gestion des User Stories, le workflow itératif s'applique à la conception d'architecture logicielle. La mise en place du service RAG (Retrieval-Augmented Generation) interne en est l'exemple parfait. Ce service vise à me doter d'une connaissance approfondie du projet pour accélérer le développement.

### Phase 1 : Le Problème d'Intégration

Une fois la base de connaissances créée via une tâche Gradle, la question se pose : comment puis-je, en tant que Gemini-CLI, l'interroger de manière performante pendant nos conversations ?

*   **Contrainte :** Je ne peux exécuter que des commandes shell.
*   **Problème de performance :** Lancer une tâche Gradle pour chaque question introduirait une latence de plusieurs secondes, ce qui est inacceptable pour un dialogue fluide.

### Phase 2 : Conception de l'Architecture

La solution retenue est une **architecture client/serveur découplée**, conçue pour la réactivité.

1.  **Le Serveur RAG :**
    *   **Technologie :** La logique RAG sera intégrée directement dans l'application **Kotlin/Spring Boot réactive** existante.
    *   **Rôle :** Exposer un point de terminaison HTTP qui reçoit une question, interroge la base de données PostgreSQL (`pgvector`), et retourne un contexte structuré.

2.  **Le Client (`query-kb`) :**
    *   **Technologie :** Un simple outil en ligne de commande (CLI) en Kotlin.
    *   **Rôle :** Agir comme un **pont** entre moi et le serveur. Je l'appelle via `run_shell_command`. Il envoie la question au serveur Spring Boot et m'imprime la réponse.

### Phase 3 : Formalisation du Dialogue (Modèle Contexte Protocole - MCP)

Pour rendre la communication intelligente, un protocole (contrat) JSON est défini.

*   **Requête MCP (Client -> Serveur) :** Permet de spécifier la question, le nombre de résultats attendus, et de filtrer les sources.
    ```json
    {
      "query": "Détails sur la refactorisation de createCnameFile",
      "top_k": 5,
      "source_filter": ["docs", "code"]
    }
    ```
*   **Réponse MCP (Serveur -> Client) :** Retourne une liste de résultats avec des métadonnées riches (source, score de similarité, type de contenu).
    ```json
    {
      "results": [
        {
          "source": "iterative_development_guide.md",
          "content": "...",
          "similarity_score": 0.92,
          "metadata": { "type": "documentation" }
        }
      ]
    }
    ```

### Phase 4 : Industrialisation et Déploiement (Jib)

Pour automatiser la construction et le déploiement du serveur Spring Boot, le plugin **Jib** est utilisé.

*   **Principe :** Jib construit une image de conteneur optimisée pour l'application Spring Boot directement depuis Gradle, sans nécessiter de Dockerfile.
*   **Principe Clé :** L'architecture conteneurisée suit la règle **"un service par conteneur"**. Jib construit l'image de l'application ; la base de données PostgreSQL tourne dans un conteneur séparé.

#### Workflow de Déploiement Local (avec Portainer)

1.  **Construction :** La commande `./gradlew jibDockerBuild` construit l'image et la charge dans le daemon Docker local.
2.  **Orchestration :** Portainer est utilisé pour lancer et gérer les deux conteneurs (l'application RAG et la base de données PostgreSQL), en s'assurant qu'ils communiquent sur le même réseau Docker.

#### Workflow de CI/CD (avec GitHub Actions)

1.  **Construction et Publication :** Une GitHub Action est déclenchée sur un push. Elle exécute la commande `./gradlew jib`, qui construit l'image et la publie sur un registre de conteneurs (ex: GitHub Container Registry).
2.  **Déploiement Continu :** Un outil comme **Watchtower** (géré via Portainer) peut alors surveiller le registre. Lorsqu'une nouvelle image est publiée, il la télécharge automatiquement et redéploie le conteneur du service RAG.

Ce cas pratique illustre comment notre boucle itérative s'étend de la simple modification de code à la conception et à l'industrialisation d'une architecture complète.

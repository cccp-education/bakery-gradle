# Plan de Refactorisation Itérative du Script de Build

Ce document détaille le plan de refactorisation du script `build.gradle.kts`. L'objectif est de migrer la logique de publication vers `buildSrc` pour améliorer la clarté, la maintenabilité et les performances du build.

Conformément aux bonnes pratiques de développement, nous adoptons une **approche itérative et incrémentale**. Plutôt qu'une refactorisation massive et risquée, nous allons procéder fonction par fonction, en nous assurant que le build reste stable et fonctionnel après chaque micro-étape.

## Problèmes à Résoudre

1.  **Duplication de Code :** Les fonctions de publication pour le site (`pushPage`) et la maquette (`pushMaquette`) sont quasi identiques (`initAddCommit`/`initAddCommitForMaquette`, `push`/`pushMaquette`, etc.).
2.  **Dépendance à une Variable Globale :** La logique actuelle dépend de la variable `siteConfiguration` définie dans la portée globale du script, ce qui empêche un déplacement direct vers `buildSrc`.

---

## Principe Directeur : Atomicité et Validation Systématique

Chaque étape de refactorisation, qu'elle soit listée ci-dessous ou non, doit respecter une règle fondamentale pour minimiser les risques et garantir la stabilité :

**Après chaque modification atomique (renommage, déplacement de fonction, changement de signature), la tâche `./gradlew check` doit être exécutée et réussir.**

Cette boucle de feedback rapide (`Modifier -> Valider`) est non négociable. Elle prévient l'accumulation d'erreurs et assure que chaque petit pas est un pas en avant sur une base stable.

---

## Le Plan d'Action Itératif

### Itération 1 : Factoriser la logique de `commit`

1.  **Action :** Créer une nouvelle fonction générique `initAddCommitGeneric` dans `build.gradle.kts`. Cette fonction prendra la configuration de push (`GitPushConfiguration`) en paramètre, éliminant ainsi la duplication.
2.  **Adaptation :** Modifier les anciennes fonctions `initAddCommit` et `initAddCommitForMaquette` pour qu'elles appellent simplement cette nouvelle fonction avec la configuration appropriée (`siteConfiguration.pushPage` ou `siteConfiguration.pushMaquette`).
3.  **Vérification :** Lancer une tâche de publication (ex: `publishSite` ou `publishMaquette`) pour valider que le processus de commit fonctionne toujours comme avant. Le build est stable.

### Itération 2 : Factoriser la logique de `push`

1.  **Action :** Créer une nouvelle fonction générique `pushGeneric` dans `build.gradle.kts` qui accepte `GitPushConfiguration` en paramètre.
2.  **Adaptation :** Modifier les anciennes fonctions `push` et `pushMaquette` pour qu'elles deviennent de simples wrappers appelant `pushGeneric`.
3.  **Vérification :** Valider à nouveau avec une tâche de publication. Le build reste stable.

### Itération 3 : Factoriser la logique de publication de haut niveau

1.  **Action :** Créer la fonction de haut niveau `publishArtifact` dans `build.gradle.kts`. Elle prendra en paramètre la `GitPushConfiguration`, le chemin source et le chemin cible. Elle orchestrera les appels à `createRepoDir`, `copyBakedFilesToRepo`, `initAddCommitGeneric` et `pushGeneric`.
2.  **Adaptation :** Modifier les anciennes fonctions `pushPages` et `pushMaquette` (celles avec les lambdas) pour qu'elles appellent `publishArtifact`.
3.  **Vérification :** Valider une dernière fois le fonctionnement de bout en bout. Le build est toujours stable.

### Itération 4 : Nettoyage final du script `build.gradle.kts`

1.  **Action :** Maintenant que la logique est centralisée dans `publishArtifact`, modifier directement les tâches `publishSite` et `publishMaquette` pour qu'elles appellent `publishArtifact`.
2.  **Suppression :** Supprimer toutes les anciennes fonctions dupliquées et devenues inutiles (`initAddCommit`, `initAddCommitForMaquette`, `push`, `pushMaquette`, `pushPages`, etc.).
3.  **Renommage :** Renommer les fonctions génériques pour des noms plus simples (ex: `initAddCommitGeneric` -> `initAddCommit`). Le script `build.gradle.kts` est maintenant propre, sans duplication, et la logique est prête à être déplacée.
4.  **Vérification :** S'assurer que les tâches fonctionnent toujours après ce nettoyage.

### Itération 5 : Migration vers `buildSrc`

1.  **Action :** Créer le fichier `buildSrc/src/main/kotlin/com/cheroliv/logic/Publishing.kt`.
2.  **Déplacement :** Couper/coller les fonctions refactorisées (`createRepoDir`, `copyBakedFilesToRepo`, `initAddCommit`, `push`, `publishArtifact`) depuis `build.gradle.kts` vers le nouveau fichier `Publishing.kt`.
3.  **Finalisation :** Ajouter la déclaration de `package` et les `imports` nécessaires dans le nouveau fichier, et ajouter les `imports` correspondants en haut de `build.gradle.kts`.
4.  **Vérification Finale :** Lancer les tâches `publishSite` et `publishMaquette` pour confirmer que le build fonctionne parfaitement avec la logique désormais externalisée dans `buildSrc`.

Cette approche découpée garantit une progression sécurisée, une validation à chaque étape, et facilite grandement le débogage en cas de problème.

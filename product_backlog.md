@[//]: # (Le fichier @product_backlog.md  ne contient que les us en cours de traitement.)
#### P Product Backlog (All tasks)

Ce tableau représente la liste complète et priorisée des tâches à réaliser pour le projet et encore non réalisé ni en cours.

---
### **Épopée 18 : Tests du Plugin Bakery**

*   **Objectif :** Assurer la robustesse et la fiabilité du plugin `bakery` en implémentant une suite de tests unitaires et d'intégration couvrant la logique de publication.

*   **User Stories :**
    *   **US 18.1 :** En tant que développeur, je veux des tests unitaires pour les fonctions de manipulation de fichiers (`createRepoDir`, `copyBakedFilesToRepo`) afin de garantir qu'elles se comportent comme attendu dans différents scénarios (création, suppression, copie).
    *   **US 18.2 :** En tant que développeur, je veux un test d'intégration pour la fonction `publishArtifact` qui valide l'ensemble du flux de publication (création de dépôt, copie, commit, push) en utilisant un dépôt Git "remote" simulé localement, afin de tester la logique Git de manière isolée et fiable.

---
### **Épopée 17 : Refonte du Cœur de JBake avec un Plugin Gradle Indépendant**

*   **Objectif :** Développer un plugin Gradle autonome, `site-baker-core`, qui encapsule et modernise la logique de génération de site statique en utilisant directement `jbake-core`. Ce plugin remplacera à terme l'utilisation du plugin JBake officiel pour offrir plus de flexibilité, de maintenabilité et une meilleure intégration avec l'écosystème du projet.

---
### **Épopée 8 : Amélioration Continue et Refactoring Technique**

*   **Objectif :** Assurer la maintenabilité, la performance et la qualité du code à long terme en appliquant les meilleures pratiques de développement et en réduisant la dette technique.

---
*   **User Stories :**
    *   **US 8.1 :** En tant qu'utilisateur, je veux que le texte du bouton "Me Contacter" dans la section héroïque reste lisible au survol, quel que soit le thème (clair ou sombre), afin de garantir une expérience utilisateur cohérente et accessible.
        *   **Description :** Actuellement, en mode sombre, le texte du bouton "Me Contacter" devient de la même couleur que le fond du bouton au survol, le rendant illisible. Le comportement attendu est que la couleur du texte s'adapte pour contraster avec la couleur de fond du bouton au survol.
        *   **Statut :** À faire.
    *   **US 71 :** En tant que développeur, je veux analyser en profondeur le module `greeting-plugin` pour définir un standard de qualité et des bonnes pratiques pour le développement de plugins Gradle dans ce projet. Cette analyse initiale servira de base pour documenter les conventions de codage, la structure des tests (unitaires, fonctionnels, d'intégration), la gestion des dépendances et la publication. L'objectif est de créer un "template" de référence pour tous les futurs plugins Gradle.
        *   **Statut :** En cours.
        *   **Avancement :**
            *   L'analyse du module `greeting-plugin` est terminée et il a été choisi comme template de référence.
            *   La migration vers une architecture basée sur des plugins Gradle a commencé.
            *   Les modules `ssg-site` et `assistant` ont été créés en dupliquant `greeting-plugin` pour initier la nouvelle structure.

---
### **Épopée 7 : Intégration du Formulaire de Contact avec Supabase et Notifications GMAIL**

*   **Objectif :** Mettre en place un système complet et robuste pour le formulaire de contact, de la soumission par l'utilisateur à la notification de l'administrateur, en utilisant Supabase pour le backend et Google Apps Script pour les notifications par email.

---

#### **Phase 1 : Amélioration du Frontend (Thymeleaf & JavaScript)**

*   **Objectif :** Créer une expérience utilisateur fluide et réactive lors de la soumission du formulaire.

*   **User Stories :**
    *   **US 7.1 :** En tant que visiteur, je veux que le bouton "Envoyer" soit désactivé et affiche un indicateur de chargement après que j'ai cliqué dessus, afin de comprendre que ma requête est en cours de traitement et d'éviter les soumissions multiples. (Terminée)
    *   **US 7.2 :** En tant que visiteur, je veux voir un message de succès clair et visible directement sur la page après l'envoi réussi de mon message, pour être certain que ma demande a bien été prise en compte.
    *   **US 7.3 :** En tant que visiteur, si une erreur survient lors de l'envoi de mon message, je veux voir une notification d'erreur explicite pour savoir que je dois réessayer ou contacter le support autrement.
    *   **US 7.4 :** En tant que développeur, je veux implémenter un client JavaScript (`contact.js`) qui intercepte la soumission du formulaire pour appeler la fonction RPC de Supabase de manière asynchrone.
    *   **US 7.5 :** En tant que développeur, je veux intégrer les zones de notification (pour les succès et erreurs) dans le template Thymeleaf `contact_section.thyme` et lier le nouveau script `contact.js`.
    *   **US 7.13 :** En tant que développeur, je veux implémenter la validation du formulaire de contact côté client en utilisant JavaScript, afin de vérifier les champs obligatoires et le format de l'email avant toute soumission, en affichant des messages d'erreur clairs sous les champs concernés.

---

#### **Phase 2 : Automatisation des Notifications (Google Apps Script)**

*   **Objectif :** Assurer que l'administrateur et l'utilisateur reçoivent une confirmation rapide et fiable de chaque nouveau message.

*   **User Stories :**
    *   **US 7.6 :** En tant qu'administrateur, je veux recevoir une notification par email (via Google Apps Script) chaque fois qu'un nouveau message est soumis via le formulaire de contact, afin de pouvoir y répondre rapidement.
    *   **US 7.7 :** En tant que visiteur, je veux recevoir un email d'accusé de réception après avoir soumis un message, pour confirmer que ma demande a été reçue et est en cours de traitement.
    *   **US 7.8 :** En tant que développeur, je veux créer un script Google Apps qui se connecte à Supabase à intervalles réguliers (toutes les 15 minutes) pour vérifier les nouveaux messages non traités.
    *   **US 7.9 :** En tant que développeur, je veux mettre en place une fonction `initialSetup` dans le script pour faciliter la configuration des permissions et la création du déclencheur (trigger) temporel.

---

#### **Phase 3 : Déploiement et Tests End-to-End**

*   **Objectif :** Garantir la fiabilité et la sécurité du système complet en production.

*   **User Stories :**
    *   **US 7.10.1 :** En tant que développeur, je veux refactoriser la logique de build JBake hors du script principal et l'encapsuler dans un plugin Gradle dédié (`site-baker`), afin de centraliser la configuration, d'améliorer la maintenabilité et de fiabiliser les tests.
        *   **Statut :** En cours.
        *   **Bilan et Plan d'Action (18/10/2025) :**
            *   **Observations sur l'Avancement :** La migration de la logique de build vers un plugin externe est en cours. Le plugin initialement nommé `ssg-site` a été renommé `site-baker` pour mieux refléter son rôle.
            *   **Diagnostic du Blocage Précédent :** L'approche initiale utilisant `includeBuild` pour intégrer le plugin a révélé un conflit structurel profond avec le répertoire spécial `buildSrc` de Gradle, causant des échecs de build liés au catalogue de versions.
            *   **Décision Stratégique :** Pour résoudre ce conflit de manière définitive et garantir un découplage propre, le plugin `site-baker` a été extrait dans un projet Gradle **totalement indépendant**. Il n'est plus un sous-projet ou un build composite, mais un artefact qui sera publié (sur `mavenLocal` pour le développement) et consommé comme une dépendance de plugin standard. Cette architecture élimine les conflits de classpath et de catalogue de versions.
            *   **Plan d'Action :**
                1.  Finaliser le développement du plugin `site-baker` dans son propre projet, avec une couverture de tests complète (unitaires et fonctionnels).
                2.  Publier le plugin sur `mavenLocal` pour le rendre disponible au projet principal.
                3.  Mettre à jour le build du projet principal pour appliquer et configurer le plugin `com.cheroliv.site.baker` depuis `mavenLocal`.
                4.  Valider l'intégration de bout en bout en exécutant les tâches fournies par le plugin (`bake`, `serve`, etc.).
                5.  Reprendre la création des tests fonctionnels pour le formulaire de contact, en s'appuyant sur le nouveau système de build.
    *   **US 7.10 :** En tant que développeur, je veux réaliser un test complet du parcours utilisateur : de la saisie dans le formulaire à la réception des emails de notification et d'accusé de réception.
        *   **Revue de Code et Plan d'Action (18/09/2025) :**
            *   **Bilan :** L'analyse du code révèle une bonne base de tests de validation (`ContactFormValidationTest.kt`) mais une déconnexion majeure avec le code applicatif (`contact.js`), qui est incomplet. Le test d'UI (`ContactFormUITest.kt`) échoue car il teste des fonctionnalités (ex: état de chargement du bouton) non encore implémentées.
            *   **Contraintes / Points Faibles :**
                1.  **Logique JavaScript manquante :** `contact.js` ne gère pas la soumission asynchrone, le changement d'état du bouton, ni l'affichage des notifications de succès/erreur.
                2.  **HTML incomplet :** `contact_section.thyme` ne contient pas les conteneurs pour afficher les messages à l'utilisateur.
                3.  **Duplication de code :** La gestion du serveur de test est dupliquée dans les deux fichiers de test.
            *   **Points Forts :**
                1.  **Tests de validation exhaustifs.**
                2.  **Gestion du serveur de test robuste (logique PID).**
            *   **Plan d'Action :**
                1.  **Étape 1 :** Mettre à jour `contact_section.thyme` avec des zones de notification.
                2.  **Étape 2 :** Compléter `contact.js` pour implémenter la logique de soumission complète.
                3.  **Étape 3 :** Refactoriser les tests pour mutualiser la gestion du serveur.
        *   **Revue de Code et Plan d'Action (18/09/2025) :**
            *   **Bilan :** L'analyse du code révèle une bonne base de tests de validation (`ContactFormValidationTest.kt`) mais une déconnexion majeure avec le code applicatif (`contact.js`), qui est incomplet. Le test d'UI (`ContactFormUITest.kt`) échoue car il teste des fonctionnalités (ex: état de chargement du bouton) non encore implémentées.
            *   **Contraintes / Points Faibles :**
                1.  **Logique JavaScript manquante :** `contact.js` ne gère pas la soumission asynchrone, le changement d'état du bouton, ni l'affichage des notifications de succès/erreur.
                2.  **HTML incomplet :** `contact_section.thyme` ne contient pas les conteneurs pour afficher les messages à l'utilisateur.
                3.  **Duplication de code :** La gestion du serveur de test est dupliquée dans les deux fichiers de test.
            *   **Points Forts :**
                1.  **Tests de validation exhaustifs.**
                2.  **Gestion du serveur de test robuste (logique PID).**
            *   **Plan d'Action :**
                1.  **Étape 1 :** Mettre à jour `contact_section.thyme` avec des zones de notification.
                2.  **Étape 2 :** Compléter `contact.js` pour implémenter la logique de soumission complète.
                3.  **Étape 3 :** Refactoriser les tests pour mutualiser la gestion du serveur.
    *   **US 7.11 :** En tant que développeur, je veux externaliser les clés d'API et URLs Supabase dans des variables d'environnement ou un fichier de configuration sécurisé pour faciliter le passage de l'environnement de test à la production.
    *   **US 7.12 :** En tant qu'administrateur, je veux pouvoir consulter les logs d'exécution du Google Apps Script pour diagnostiquer rapidement les éventuelles erreurs de notification.

---
### **Épopée 15 : Création de Contenu : Livre Gradle et Articles Teasers**

*   **Objectif :** Écrire un livre sur Gradle, le teaser avec des articles, et le diffuser sur le site JBake avec sa propre représentation et son templating Thymeleaf dédié.

*   **Avancement (25/09/2025) :**
    *   Le plugin `site-baker` a été initialisé avec une structure de tests robuste (unitaires et fonctionnels) suivant une approche TDD.
    *   Les tests valident l'enregistrement de la tâche, la configuration de l'extension DSL, et la capacité à lire un fichier de configuration.
    *   Cette base solide va permettre d'intégrer la logique de parsing YAML (US 15.1) en toute confiance.

*   **User Stories :**
    *   **US 15.1 :** En tant que développeur, je veux intégrer la bibliothèque Jackson dans le plugin `site-baker` pour lire et parser le fichier de configuration YAML (défini via le DSL `site.configPath`). Je réutiliserai le code de parsing existant du projet principal pour accélérer le développement.

*   **Articles à écrire :**
    *   [ ] Article 0 : Mise en place de l'environnement d'exécution et de dev
    *   [ ] Article 1 : Introduction aux plugins Gradle
    *   [ ] Article 2 : Créer son premier plugin Gradle
    *   [ ] Article 3 : Tester un plugin Gradle
    *   [ ] Article 4 : Publier un plugin Gradle
    *   [ ] Article 5 : Développer un plugin Gradle avec une approche TDD

*   **Plan du livre :** Voir le document dédié : `book-plugin/gradle-plugin-book.adoc`.

*   **Contraintes :**
    *   Journaliser les prompts, réponses, le moteur et le contexte dans un appel base de données.

*   **Structure du projet :**
    *   `site-baker/` : Répertoire pour le code du plugin Gradle (projet Gradle plugin initialisé).

    *   **US 72 :** En tant qu'utilisateur, je veux que les diagrammes s'adaptent automatiquement au thème (clair ou sombre) du site pour une lisibilité optimale.
        *   **Description :** Le cœur du problème est que PlantUML génère un SVG avec des couleurs **fixes et codées en dur**. Ce SVG ne sait pas si votre site est en mode clair ou sombre. La stratégie recommandée consiste à générer un **seul SVG standard** et à le modifier dynamiquement dans le navigateur de l'utilisateur (côté client).
            *   **Comment ça marche :** Un SVG est un fichier texte (XML) dont les éléments (`<path>`, `<rect>`, `<text>`) peuvent être stylisés avec du CSS. L'idée est de ne pas coder les couleurs en dur dans le SVG, mais d'utiliser des variables CSS.
                1.  **Génération :** Configurer PlantUML pour qu'il génère un SVG "brut", sans couleurs de fond.
                2.  **Stylisation :** Utiliser le CSS du site pour cibler les éléments à l'intérieur du SVG. Le même mécanisme qui gère le mode clair/sombre du site peut ainsi changer les couleurs du diagramme.
            *   **Avantages :**
                *   **Solution unique et propre :** Un seul fichier image par diagramme.
                *   **Adaptation parfaite :** Le diagramme change de thème instantanément avec le reste du site.
                *   **Maintenance facile :** Toutes les couleurs sont gérées dans le fichier CSS principal.
            *   **Prérequis :**
                *   **Le SVG doit être "inline" :** Pour que le CSS de la page puisse affecter un SVG, celui-ci doit être directement intégré dans le code HTML (balise `<svg>...</svg>`) et non chargé via une balise `<img>`. Cela nécessite une étape de post-traitement après le `bake` pour lire les fichiers SVG et les injecter dans le HTML.
        *   **Analyse de Décision Technique (07/10/2025) :**
            *   **Contexte :** Une hésitation a été soulevée quant à la charge de travail imposée au navigateur du client ("fatigue") par la manipulation de CSS sur les SVG, par opposition à la génération de plusieurs images côté serveur.
            *   **Analyse de l'approche "Côté Client" (Manipulation CSS) :**
                *   **Coût de performance :** La manipulation de CSS pour changer les couleurs d'un SVG est une opération **triviale** pour les moteurs de rendu des navigateurs modernes. C'est une de leurs fonctions principales et elle est extrêmement optimisée. L'impact sur le processeur est infime, non perceptible, et des milliers de fois moins coûteux que des opérations courantes comme le décodage vidéo ou l'exécution de JavaScript lourd.
                *   **Standardisation :** Styliser des SVG avec du CSS est une pratique web **standard, robuste et recommandée**, utilisée par de nombreux sites majeurs. Ce n'est pas une technique "suspecte" ou un "hack".
                *   **Performance comparée :** Le navigateur télécharge un seul fichier SVG. Le changement de thème est quasi-instantané via le moteur CSS. L'approche multi-images, quant à elle, peut impliquer le téléchargement d'une seconde image, augmentant potentiellement le coût réseau.
            *   **Analyse de l'approche "Côté Serveur" (Multiples Images) :**
                *   **Temps de build :** Le processus de build est allongé car il doit générer deux fois plus d'images.
                *   **Complexité du build :** La logique de build doit être modifiée pour générer des balises HTML `<picture>` au lieu de `<img>`, ce qui est complexe à intégrer dans la chaîne JBake/Asciidoctor.
                *   **Maintenance :** La maintenance est plus lourde. Un changement de couleur nécessite de modifier deux thèmes PlantUML. L'ajout d'un troisième thème (ex: "sépia") obligerait à tout tripler.
            *   **Tableau Comparatif :**
| Critère | Approche Côté Client (CSS/Inline SVG) | Approche Côté Serveur (Multiples Fichiers) |
| :--- | :--- | :--- |
| **Performance (Navigateur)** | **Excellente.** Impact CPU/mémoire négligeable. | **Très bonne.** Le navigateur choisit une image optimisée. |
| **Performance (Build)** | **Rapide.** Une seule image par diagramme. | **Lente.** Deux fois plus d'images à générer. |
| **Maintenance / Complexité** | **Faible.** Tout est centralisé dans le CSS. | **Élevée.** Logique de build complexe, thèmes dupliqués. |
| **Flexibilité** | **Très élevée.** Ajouter un nouveau thème est trivial. | **Très faible.** Chaque nouveau thème double la complexité. |
| **Qualité du Rendu** | **Parfaite.** Le diagramme est un citoyen de première classe du DOM. | **Parfaite.** L'image est pré-rendue pour le thème. |
| **Effort d'implémentation initial** | **Moyen.** Nécessite un script de post-traitement pour "inliner" les SVG. | **Élevé.** Nécessite de modifier la logique de génération HTML. |
            *   **Décision Finale :** L'approche **Côté Client (CSS + SVG inline)** est choisie. Elle est techniquement supérieure, plus maintenable, plus flexible et plus performante du point de vue du build, sans impact négatif notable sur les performances du navigateur. L'effort initial de mise en place du script de post-traitement est justifié par les gains à long terme.
        *   **Plan d'Implémentation Technique (Révisé le 07/10/2025) :**
            *   **Principe :** Pour garantir un build autonome et reproductible, la solution s'appuiera sur le plugin `com.github.node-gradle.node`. Cela évite de dépendre d'une installation manuelle de Node.js sur l'environnement de développement ou de CI/CD. Le script de transformation sera exécuté par une tâche Gradle de type `NodeTask`.
            *   **Étape 1 : Intégration du `gradle-node-plugin`**
                1.  Modifier `build.gradle.kts` pour y ajouter le plugin `com.github.node-gradle.node`.
                2.  Configurer le plugin dans ce même fichier pour qu'il télécharge et utilise une version LTS récente de Node.js.
            *   **Étape 2 : Création du script de post-traitement (`inline-svgs.js`)**
                1.  Créer le répertoire `scripts/` à la racine du projet.
                2.  Créer le fichier `scripts/inline-svgs.js` avec la logique suivante :
                    *   Parcourir les fichiers HTML dans `build/jbake`.
                    *   Trouver les balises `<img src="... .svg">`.
                    *   Lire le contenu des fichiers SVG correspondants.
                    *   Remplacer les balises `<img>` par le contenu des SVG (`<svg>...</svg>`).
                    *   Réécrire les fichiers HTML modifiés.
            *   **Étape 3 : Création et orchestration de la tâche Gradle**
                1.  Dans `build.gradle.kts`, créer une nouvelle tâche de type `NodeTask` (fournie par le plugin), nommée `inlineSvgPostBake`.
                2.  Configurer cette tâche pour qu'elle exécute le script `scripts/inline-svgs.js`.
                3.  Orchestrer l'exécution pour que `inlineSvgPostBake` s'exécute impérativement après la tâche `bake` (via `finalizedBy`).
---

### **Épopée 16 : Initialisation de Site Statique via Templates**

*   **Objectif :** Fournir un moyen rapide et standardisé de créer un nouveau squelette de site statique JBake à partir de modèles prédéfinis, en utilisant une tâche Gradle dédiée.

---
*   **User Stories :**
    *   **US 16.1 :** En tant que développeur, je veux une tâche Gradle (`bakeInit`) qui initialise une nouvelle structure de site JBake en copiant un template prédéfini (ex: `example_project_freemarker.zip`) depuis les ressources du plugin (`src/main/resources/jbake-templates/`) vers le répertoire source du site, afin de démarrer rapidement un nouveau projet de site avec une base cohérente.
    *   **US 16.2 :** En tant que développeur, je veux une tâche Gradle pour packager le répertoire de site actuel (`site/`) en un nouveau template de projet JBake partageable (format zip), afin de pouvoir réutiliser la structure et le style actuels comme base pour de futurs projets ou pour le partager avec d'autres.
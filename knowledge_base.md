## 🧠 Knowledge Base

Cette section documente le parcours de développement, les décisions techniques et les solutions mises en œuvre.

### Gradle : Relancer les Tâches et Tests

*   **Contexte :** Lors du développement de la logique de build dans `buildSrc`, il est souvent nécessaire de forcer la réexécution des tâches Gradle, y compris les tests, même si Gradle les considère comme "UP-TO-DATE".
*   **Problématique :** L'option `--rerun-test` n'est pas une option valide pour la tâche `test` de Gradle.
*   **Solution :** Pour forcer la réexécution de toutes les tâches (y compris les tests) qui ne sont pas "UP-TO-DATE", l'option correcte à utiliser est `--rerun-tasks`.
*   **Exemple de commande :**
    ```bash
    ./gradlew :buildSrc:test --rerun-tasks
    ```
    Cette commande garantit que toutes les tâches du module `buildSrc` (y compris la tâche `test`) seront exécutées, même si leurs entrées n'ont pas changé.

### **Épopée 10 : Intégration du Formulaire de Contact avec Supabase**

*   **US-SUPA-CONN-01 : Dépendances Gradle et API Client pour Supabase-kt**
    *   **Contexte :** La mise en place d'un test de connexion à Supabase a révélé des points critiques concernant la déclaration des dépendances Gradle et l'utilisation de l'API client `supabase-kt`.
    *   **Leçons Apprises :**
        1.  **Coordonnées Maven Exactes :** Il est impératif d'utiliser les coordonnées Maven précises pour les dépendances Supabase. Une erreur dans le `groupId` (par exemple, `io.github.jan-tennert.supabase` est correct, tandis que `io.github.jan-tennert:supabase` ne l'est pas) empêche Gradle de résoudre les artéfacts. La source de vérité est la documentation officielle ou le dépôt Maven.
        2.  **API Client : `.auth` vs `.gotrue` :** Pour interagir avec le service d'authentification, la méthode d'accès correcte sur l'objet client Supabase est `.auth`. L'utilisation de `.gotrue` (une ancienne convention ou un nom interne) entraîne des erreurs de compilation (`Unresolved reference`).
    *   **Configuration de Référence (`buildSrc/build.gradle.kts`) :**
        ```kotlin
        val supabaseVersion = "2.6.1"
        // ...
        implementation(platform("io.github.jan-tennert.supabase:bom:$supabaseVersion"))
        implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabaseVersion")
        implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion")
        // ...
        ```
    *   **Import et Utilisation de Référence (`*.kt`) :**
        ```kotlin
        import io.github.jan.supabase.gotrue.auth
        // ...
        val session = supabase.auth.currentSessionOrNull()
        ```

*   **US-60 : Configuration du Backend Supabase**
    *   **Schéma de la Base de Données :** Pour garantir une structure de données robuste, sécurisée et normalisée, la configuration se fait via l'éditeur SQL de Supabase. Le script ci-dessous est la source de vérité pour l'architecture de la base de données.
        
        *   **Logique du Schéma :**
            1.  **Table `contacts` :** Stocke les informations uniques sur les personnes qui entrent en contact (nom, email, téléphone). L'unicité est garantie sur l'email et le téléphone.
            2.  **Table `messages` :** Stocke chaque message individuel, lié à un contact via une clé étrangère. Cela évite la duplication des données de contact si une même personne envoie plusieurs messages.
            3.  **Sécurité (RLS) :** La sécurité au niveau des lignes (Row Level Security) est activée sur les deux tables pour empêcher toute lecture ou écriture non autorisée depuis le client. L'accès aux données est entièrement contrôlé par les fonctions RPC.
            4.  **Fonction RPC `handle_contact_form` :** C'est le **seul point d'entrée** pour le formulaire de contact. Cette fonction, exécutée avec les droits `SECURITY DEFINER`, gère la logique de "trouver ou créer" un contact, puis d'insérer le message de manière atomique. Le frontend n'appelle que cette fonction (via le rôle `anon`), jamais les tables directement.
            5.  **Fonction RPC `get_schemas` :** Une fonction utilitaire sécurisée pour lister les schémas de la base de données, principalement à des fins de test et de maintenance. Elle est également accessible au rôle `anon`.
        
        *   **Script d'Initialisation SQL :**
        
        ```sql
        -- =================================================================
        --  SCHEMA COMPLET ET SÉCURISÉ POUR LE FORMULAIRE DE CONTACT
        -- =================================================================

        -- 1. Création de la table "contacts"
        -- Utilise un UUID comme clé primaire.
        -- Garantit l'unicité sur 'email' (si non nul) ET sur 'telephone' (si non nul).
        -- Assure qu'au moins l'un des deux est fourni.
        -- =================================================================
        CREATE TABLE public.contacts (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
            name TEXT,
            email TEXT,
            telephone TEXT,
            CONSTRAINT contacts_email_key UNIQUE (email),
            CONSTRAINT contacts_telephone_key UNIQUE (telephone),
            CONSTRAINT check_contact_info CHECK (email IS NOT NULL OR telephone IS NOT NULL)
        );

        -- 2. Création de la table "messages" pour stocker les soumissions
        -- =================================================================
        CREATE TABLE public.messages (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
            contact_id UUID NOT NULL REFERENCES public.contacts(id) ON DELETE CASCADE,
            subject TEXT,
            message TEXT
        );

        -- 3. Activation de la sécurité RLS pour les deux tables
        -- =================================================================
        ALTER TABLE public.contacts ENABLE ROW LEVEL SECURITY;
        ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

        -- 4. Création de la fonction RPC "handle_contact_form"
        -- C'est le seul point d'entrée pour le formulaire. Elle gère la logique
        -- de recherche/création du contact et l'insertion du message de manière atomique et sécurisée.
        -- =================================================================
        CREATE OR REPLACE FUNCTION public.handle_contact_form(
            p_name TEXT,
            p_email TEXT,
            p_subject TEXT,
            p_message TEXT
        )
        RETURNS void
        LANGUAGE plpgsql
        SECURITY DEFINER
        AS $
        DECLARE
            contact_uuid UUID;
        BEGIN
            SELECT id INTO contact_uuid FROM public.contacts WHERE email = p_email;

            IF contact_uuid IS NULL THEN
                INSERT INTO public.contacts (name, email)
                VALUES (p_name, p_email)
                RETURNING id INTO contact_uuid;
            END IF;

            INSERT INTO public.messages (contact_id, subject, message)
            VALUES (contact_uuid, p_subject, p_message);
        END;
        $;

        -- 5. Donner la permission d'exécuter la fonction au rôle public
        -- =================================================================
        GRANT EXECUTE
        ON FUNCTION public.handle_contact_form(TEXT, TEXT, TEXT, TEXT)
        TO anon;

        -- 6. Création de la fonction RPC "get_schemas"
        -- Permet de lister les schémas de la base de données de manière sécurisée.
        -- =================================================================
        CREATE OR REPLACE FUNCTION get_schemas()
        RETURNS TABLE(schema_name TEXT) AS $
        BEGIN
            RETURN QUERY SELECT nspname::TEXT FROM pg_namespace
            WHERE nspname NOT LIKE 'pg_%'
              AND nspname NOT LIKE 'crdb_%'
              AND nspname != 'information_schema'
              AND nspname != 'supabase_migrations';
        END;
        $ LANGUAGE plpgsql;

        -- 7. Donner la permission d'exécuter la fonction au rôle public
        -- =================================================================
        GRANT EXECUTE ON FUNCTION get_schemas() TO anon;
        ```

### **CI/CD : Déploiement Automatisé avec GitHub Actions**

### **Validation du Schéma Supabase : Approche Manuelle et Vérification Automatisée**

*   **Contexte :** L'initialisation du schéma de la base de données (tables, fonctions, permissions) est une étape critique. L'automatiser via une tâche Gradle s'est avéré complexe et potentiellement risqué, car cela donnerait au build des permissions élevées sur la base de données de production.
*   **Décision Stratégique :** L'approche retenue sépare la gestion de l'infrastructure de la logique applicative.
    1.  **Déploiement Manuel du Schéma :** Le schéma de la base de données est considéré comme faisant partie de l'infrastructure. Sa création et ses mises à jour sont gérées manuellement par un développeur directement dans le dashboard SQL de Supabase. Le script `supabase_script.sql` sert de source de vérité pour cette configuration.
    2.  **Vérification Automatisée dans le Build :** Pour s'assurer que l'application se déploie sur un environnement conforme, une suite de tests d'intégration a été ajoutée au module `buildSrc`. Ces tests ne modifient pas le schéma, mais le valident.
*   **Avantages :**
    *   **Sécurité :** Les identifiants utilisés par le build n'ont pas besoin de permissions pour modifier le schéma (CREATE, ALTER), réduisant la surface d'attaque.
    *   **Fiabilité :** Le build agit comme un "test de conformité" de l'environnement. S'il passe, on a une haute confiance que l'infrastructure est prête.
    *   **Clarté :** La responsabilité de la gestion du schéma est clairement définie (développeur/DBA) et découplée du cycle de vie du déploiement applicatif.
*   **Implémentation de Référence (`buildSrc/src/test/kotlin/site/core/CoreTest.kt`) :**
    Les tests utilisent le client `supabase-kt` pour interroger l'état de la base de données :
    *   Ils vérifient que les tables (`contacts`, `messages`) existent en tentant une lecture (`select`). La politique RLS doit retourner une liste vide, ce qui confirme à la fois l'existence de la table et sa protection.
    *   Ils appellent les fonctions RPC (`get_schemas`, `handle_contact_form`) pour s'assurer qu'elles sont bien déployées et accessibles avec les permissions attendues.

Cette stratégie fournit un compromis pragmatique entre l'automatisation complète et la sécurité, garantissant des déploiements robustes.

*   **US 7.1 : Implémentation du Formulaire de Contact (Frontend)**
    *   **Contexte :** L'objectif était de fournir un formulaire de contact clair et accessible, s'intégrant au nouveau design Bootstrap 5.
    *   **Implémentation :**
        1.  **Template Thymeleaf (`contact_section.thyme`) :** Le formulaire a été structuré en utilisant les classes de formulaire de Bootstrap 5 (`.form-control`, `.form-label`, `.mb-3`).
        2.  **Champs du Formulaire :** Inclusion des champs pour le nom, l'email, le sujet et le message.
        3.  **Accessibilité :**
            *   Chaque champ de saisie est associé à une balise `<label>` via l'attribut `for` pour améliorer la sémantique et l'accessibilité pour les lecteurs d'écran.
            *   Des attributs `aria-label` ou `aria-describedby` peuvent être ajoutés si des instructions supplémentaires sont nécessaires pour les utilisateurs de technologies d'assistance.
        4.  **Responsivité :** L'utilisation des classes Bootstrap 5 assure que le formulaire s'adapte correctement aux différentes tailles d'écran.
        5.  **Préparation pour le Backend :** Le formulaire est conçu pour être facilement intégré avec une solution backend (comme Supabase) pour la soumission des données.
    *   **Statut :** Terminé

Le projet intègre une pipeline de déploiement continu (CI/CD) via GitHub Actions, définie dans le fichier `.github/workflows/website.yml`. Cette automatisation assure que chaque modification poussée sur la branche `main` est automatiquement construite et déployée sur GitHub Pages.

*   **Déclencheurs (Triggers) :**
    *   **`push: branches: [main]` :** Le déploiement est lancé automatiquement à chaque fois qu'un commit est poussé sur la branche `main`.
    *   **`workflow_dispatch` :** Permet de lancer manuellement le déploiement depuis l'onglet "Actions" de GitHub, ce qui est utile pour redéployer une version existante sans avoir à créer un nouveau commit.

*   **Processus du Job (`publish-website`) :**
    1.  **Environnement :** Le job s'exécute sur une machine virtuelle Ubuntu (`ubuntu-latest`) fournie par GitHub.
    2.  **Checkout :** La première étape (`actions/checkout@v4`) récupère le code source du dépôt.
    3.  **Setup Java :** L'environnement Java 24 (`temurin`) est ensuite configuré (`actions/setup-java@v4`), ce qui est un prérequis pour exécuter Gradle.
    4.  **Déploiement :** L'étape finale exécute deux commandes critiques :
        *   `echo "${{ secrets.CHEROLIV_MIGRATION_CONFIG }}" > managed-jbake-context.yml` : Cette commande injecte la configuration externe (credentials de déploiement, clés Supabase, etc.) depuis un secret GitHub dans un fichier `managed-jbake-context.yml`. C'est une méthode sécurisée pour fournir des informations sensibles au processus de build sans les stocker dans le code.
        *   `./gradlew -q -s publishSite` : C'est le cœur du déploiement. Elle exécute la tâche Gradle `publishSite`. Au démarrage, **le build Gradle lit le fichier `managed-jbake-context.yml` et le mappe à l'objet `SiteConfiguration`**, qui devient la source de vérité unique pour toutes les tâches (JBake, déploiement, etc.).
            *   `-q` (`--quiet`) : Réduit le bruit dans les logs en n'affichant que les erreurs.
            *   `-s` (`--stacktrace`) : Fournit une trace complète en cas d'erreur, facilitant le débogage.

### **Configuration du Déploiement (`managed-jbake-context.yml` et `SiteConfiguration`)**

Le fichier `managed-jbake-context.yml`, généré dynamiquement par la GitHub Action, est la source de vérité pour la configuration externe. Son contenu est directement mappé par le build Gradle à une `data class SiteConfiguration` (via Jackson), qui centralise tous les paramètres.

Voici un exemple anonymisé de la structure du fichier YAML :

```yaml
bake:
  srcPath: "site"
  destDirPath: "bake"
  cname: ""
pushPage:
  from: "bake"
  to: "cvs"
  repo:
    name: "jdoe.github.io"
    repository: "https://github.com/jdoe/jdoe.github.io.git"
    credentials:
      username: "cheroliv"
      password: "gho_api_key"
  branch: "main"
  message: "jdoe/jdoe.github.io"
pushMaquette:
  from: "bake"
  to: "cvs"
  repo:
    name: "jdoe-maquette"
    repository: "https://github.com/jdoe/jdoe-maquette.git"
    credentials:
      username: "jdoe"
      password: "gho_api_key"
  branch: "main"
  message: "jdoe/jdoe-maquette"
```

*   **Analyse des sections :**
    *   **`bake`** : Cette section configure les chemins de base pour JBake.
        *   `srcPath` : Le dossier source (`site/`) contenant les templates, assets et contenus.
        *   `destDirPath` : Le dossier de destination (`bake/`) où le site statique est généré par JBake.
    *   **`pushPage`** : C'est la configuration principale pour le déploiement du site public.
        *   `from` : Spécifie que le contenu à déployer se trouve dans le dossier `bake` (le site généré).
        *   `repo` : Définit le dépôt Git de destination.
            *   `repository` : L'URL du dépôt où le site doit être publié (ex: `cheroliv.github.io`).
            *   `credentials` : Contient les informations d'authentification. Le `password` est un **Personal Access Token (PAT)** de GitHub avec les permissions nécessaires pour pousser du code.
        *   `branch` : La branche cible du déploiement (ici, `main`).
        *   `message` : Le message de commit utilisé pour le déploiement.
    *   **`pushMaquette`** : Cette section illustre la flexibilité du script, permettant de déployer potentiellement une autre version du site (une maquette) vers un dépôt différent avec ses propres credentials et message de commit.

Cette approche centralisée et basée sur les secrets est une bonne pratique de sécurité qui sépare la configuration sensible du code source versionné.

### **Épopée 11 : Couverture de Test pour le Build Gradle (Infrastructure)**
*   **US-62 : Initialisation de `buildSrc` pour la Logique de Build**
    > **En tant que développeur, je veux initialiser le dossier `buildSrc` et son fichier `build.gradle.kts` avec un code commenté, afin de préparer l'environnement pour le développement de plugins Gradle personnalisés et de valider l'absence de régressions.**
    *   **Description :** Création du dossier `buildSrc/` et du fichier `buildSrc/build.gradle.kts` avec un contenu entièrement commenté. Cette étape permet de vérifier que l'ajout de ce module de build n'introduit pas de régressions dans le projet principal et que Gradle le reconnaît correctement. C'est la première étape pour mettre en place un environnement d'itération TDD pour la logique de build.
    *   **Étape 2 : Activation du plugin `kotlin-dsl`**
        *   **Action :** Décommenter la ligne `plugins { `kotlin-dsl` }` dans `buildSrc/build.gradle.kts`.
        *   **Vérification :** Lancer la tâche `./gradlew -q -s check --rerun-tasks` pour s'assurer que l'activation du plugin ne cause pas de régressions et que le projet compile toujours correctement.
    *   **Étape 3 : Ajout des dépôts Maven**
        *   **Action :** Décommenter le bloc `repositories { ... }` dans `buildSrc/build.gradle.kts` pour inclure les dépôts nécessaires (Google, Maven Central, Gradle Plugin Portal, et d'autres dépôts personnalisés).
        *   **Vérification :** Lancer la tâche `./gradlew -q -s check --rerun-tasks` pour confirmer que l'ajout des dépôts ne génère pas d'erreurs et que Gradle peut résoudre les dépendances.
    *   **Étape 4 : Ajout des dépendances de la logique de build (avec versions explicites et correction `implementation` et `Project.versions`)**
        *   **Action :** Décommenter le bloc `val Project.versions { ... }` et le second bloc `dependencies { ... }` dans `buildSrc/build.gradle.kts`. Les dépendances `libs.xxx` ont été remplacées par leurs chaînes de caractères complètes avec les versions explicites. La méthode `classpath(it)` a été remplacée par `implementation(it)` car `classpath` n'est pas valide dans le bloc `dependencies` d'un plugin `kotlin-dsl`. Le bloc `val Project.versions` a été décommenté et la faute de frappe `langchain44j.version` a été corrigée en `langchain4j.version`. La correction manuelle de `classpath(it)` à `implementation(it)` a été effectuée.
        *   **Vérification :** Lancer la tâche `./gradlew -q -s check --rerun-tasks` pour s'assurer que l'ajout de ces dépendances ne cause pas de régressions et que le projet compile toujours correctement.
        *   **Étape 5 : Configuration des tests unitaires et fonctionnels**
        *   **Action :** Décommenter les blocs de code relatifs à la configuration des `SourceSet` pour les tests fonctionnels (`functionalTestSourceSet`), les dépendances de test (`testImplementation`, `testRuntimeOnly`), les tâches de test (`functionalTest`, `test`), les arguments JVM (`jvmArgs`) et les tâches de rapport (`reportTests`, `reportFunctionalTests`).
        *   **Vérification :** Lancer la tâche `./gradlew -q -s check --rerun-tasks` pour s'assurer que la configuration des tests est correcte et que le projet compile toujours sans régression.
        *   **Résultat :** La tâche `check` s'est exécutée avec succès, confirmant que la configuration des tests est opérationnelle.


*   **Objectif :** Mettre en place une couverture de test robuste pour le script de build Gradle, afin de garantir sa fiabilité et sa maintenabilité.
*   **Ressources :**
    *   [Testing Build Logic with TestKit](tiroir/gradle-9.0.0/docs/userguide/test_kit.html) : Documentation officielle de Gradle sur l'utilisation de TestKit pour tester la logique de build, y compris les tâches Gradle.
*   **US-67 : Intégration de JUnit 5 pour les Tests Gradle**
    > **En tant que développeur, je veux intégrer JUnit 5 au projet Gradle, afin de pouvoir écrire et exécuter des tests unitaires pour les tâches et la logique du build.**
*   **US-68 : Écriture de Tests Unitaires pour les Tâches Gradle Clés**
    > **En tant que développeur, je veux écrire des tests unitaires pour les tâches Gradle critiques (ex: `publishSite`, `setupSupabaseSchema`), afin de valider leur comportement et leurs interactions.**
*   **US-69 : Mesure de la Couverture de Code avec JaCoCo**
    > **En tant que développeur, je veux intégrer JaCoCo au build Gradle, afin de mesurer la couverture de code des scripts de build et d'identifier les zones non testées.**
*   **US-70 : Rapports de Couverture de Test dans GitHub Actions**
    > **En tant que développeur, je veux que les rapports de couverture de test soient générés et affichés dans la pipeline GitHub Actions, afin de visualiser l'état de la couverture à chaque déploiement.**

### **SVG Manipulation Utilities**

Cette section contient des utilitaires JavaScript pour la manipulation de fichiers SVG, notamment pour ajuster les couleurs d'éléments spécifiques.

#### **`changeCharacterBodyColor(svgContent, newColor)`**

*   **But :** Cette fonction JavaScript est conçue pour modifier la couleur de remplissage (fill) des éléments spécifiques qui composent le corps du personnage dans un fichier SVG. Elle identifie ces éléments par leurs attributs `d` (pour les chemins) et `x`, `y`, `width`, `height` (pour les rectangles) ainsi que leur `transform`, garantissant que seuls les éléments du corps du personnage sont ciblés, indépendamment de leur couleur initiale.
*   **Fonctionnement :**
    1.  Prend le contenu SVG sous forme de chaîne de caractères (`svgContent`) et la nouvelle couleur (`newColor`) comme arguments.
    2.  Utilise `DOMParser` pour analyser la chaîne SVG en un document XML manipulable.
    3.  Définit des listes d'objets (`characterBodyPaths` et `characterBodyRects`) contenant les attributs uniques (`d`, `transform` pour les chemins ; `x`, `y`, `width`, `height`, `transform` pour les rectangles) des éléments qui constituent le corps du personnage. Ces listes servent de référence pour l'identification.
    4.  Itère sur tous les éléments `<path>` et `<rect>` du document SVG.
    5.  Pour chaque élément, elle compare ses attributs avec ceux définis dans les listes de référence.
    6.  Si un élément correspond à un élément du corps du personnage, son attribut `fill` est mis à jour avec la `newColor` fournie.
    7.  Enfin, elle utilise `XMLSerializer` pour reconvertir le document XML modifié en une chaîne SVG, qui est ensuite retournée.

```javascript
function changeCharacterBodyColor(svgContent, newColor) {
  const parser = new DOMParser();
  const xmlDoc = parser.parseFromString(svgContent, "image/svg+xml");

  const pathElements = xmlDoc.querySelectorAll('path');
  const rectElements = xmlDoc.querySelectorAll('rect');

  const characterBodyPaths = [
    {
      d: "M849.83956,413.92064a6.5084,6.5084,0,0,1-6.49268-6.25782l-.82226-21.998a6.49954,6.49954,0,0,1,6.25293-6.7378l94.88623-3.54638a6.50038,6.50038,0,0,1,6.73828,6.25293l.82226,21.998a6.49952,6.49952,0,0,1-6.25293,6.7378l-94.88623,3.54638C850.00313,413.91917,849.92086,413.92064,849.83956,413.92064Z",
      transform: "translate(-144.19918 -224.30788)"
    },
    {
      d: "M961.031,616.7014H946.2712a6.50737,6.50737,0,0,1-6.5-6.5V487.18724a6.50736,6.50736,0,0,1,6.5-6.5H961.031a6.50736,6.50736,0,0,1,6.5,6.5V610.2014A6.50737,6.50737,0,0,1,961.031,616.7014Z",
      transform: "translate(-144.19918 -224.30788)"
    },
    {
      d: "M895.51835,551.67716l-12.62188-7.65058a6.50737,6.50737,0,0,1-2.18933-8.92787L944.47142,429.9008a6.50738,6.50738,0,0,1,8.92787-2.18933l12.62189,7.65059a6.50737,6.50737,0,0,1,2.18933,8.92787l-63.76429,105.1979A6.50735,6.50735,0,0,1,895.51835,551.67716Z",
      transform: "translate(-144.19918 -224.30788)"
    },
    {
      d: "M911.00806,318.72176a12.0954,12.0954,0,0,1-4.4169-2.59271,8.13347,8.13347,0,0,1-2.37749-6.439,5.47158,5.47158,0,0,1,2.43254-4.21108c1.8238-1.1699,4.26308-1.17336,6.74664-.07887l-.094-19.90968,1.99986-.00953.11063,23.40593-1.541-.96934c-1.78694-1.12214-4.33888-1.912-6.14208-.755a3.51372,3.51372,0,0,0-1.52155,2.71892,6.14621,6.14621,0,0,0,1.76778,4.80094c2.20841,2.10955,5.43042,2.76924,9.10469,3.35878l-.317,1.97452A32.96389,32.96389,0,0,1,911.00806,318.72176Z",
      transform: "translate(-144.19918 -224.30788)"
    },
    {
      d: "M981.19918,520.19212h-86a8.50951,8.50951,0,0,1-8.5-8.5l18.00415-138.06445a8.48983,8.48983,0,0,1,8.49585-8.43555h30.52a46.032,46.032,0,0,1,45.98,45.98v100.52A8.50951,8.50951,0,0,1,981.19918,520.19212Z",
      transform: "translate(-144.19918 -224.30788)"
    },
    {
      d: "M1035.87729,440.84144a6.51006,6.51006,0,0,1-.96748,7.00666l-16.48161,19.07669a6.50739,6.50739,0,0,1-9.16784.6689l-71.85068-62.07617a6.49946,6.49946,0,0,1-.66922-9.168l16.48116-19.07648a6.49948,6.49948,0,0,1,9.16785-.6689l71.85112,62.076A6.47185,6.47185,0,0,1,1035.87729,440.84144Z",
      transform: "translate(-144.19918 -224.30788)"
    },
    {
      d: "M939.56779,271.06053c-6.81763-5.64183-15.93126-.30583-23.64118-1.06317-7.37624-.72457-13.31317-7.21594-14.931-14.20131-1.8875-8.1496,2.20387-16.428,8.36691-21.64861,6.75006-5.71783,15.81263-7.49465,24.449-6.62458,9.89839.9972,19.01778,5.42908,27.22791,10.84328a121.85093,121.85093,0,0,1,21.78291,17.7586c5.87072,6.24127,10.88632,13.71838,12.60436,22.23434,1.56134,7.73926.64531,16.36569-3.82068,23.03188a24.20535,24.20535,0,0,1-9.3242,7.98384c-3.9,2.00789-8.07309,3.46963-11.8455,5.72774-5.70393,3.41429-11.17849,10.373-9.2366,17.46955a9.79275,9.79275,0,0,0,2.30515,4.147c1.31763,1.408,3.6093-.52921,2.28819-1.9409-2.32034-2.47942-2.29475-5.84538-1.14094-8.88016a16.27227,16.27227,0,0,1,7.08105-8.09024c3.94892-2.35837,8.32982-3.851,12.38208-6.00784a27.02248,27.02248,0,0,0,9.75361-8.40065c4.768-6.89883,6.06465-15.78389,4.7983-23.9565-1.37038-8.844-6.07919-16.81476-11.9184-23.45829-6.3547-7.23-14.10718-13.305-22.01249-18.7446-8.48247-5.83678-17.82438-10.73014-28.09011-12.36914-8.89876-1.42076-18.493-.25967-26.15637,4.72065-7.15325,4.6488-12.55687,12.51455-12.94419,21.1964a22.23536,22.23536,0,0,0,10.75321,19.74136,19.10112,19.10112,0,0,0,11.45819,2.42612c4.25565-.3537,8.49577-1.73948,12.79069-1.45871a8.94075,8.94075,0,0,1,5.30755,2.028c1.48956,1.23267,3.18992-1.24152,1.71258-2.46406Z",
      transform: "translate(-144.19918 -224.30788)"
    },
    {
      d: "M879.59864,536.87587q.02745-.16917.06422-.33783a6.45569,6.45569,0,0,1,2.83054-4.09112l12.40306-8.0001a6.50779,6.50779,0,0,1,8.98561,1.939l38.26114,59.31975a6.501,6.501,0,0,1-1.939,8.98561l-12.40353,8a6.50781,6.50781,0,0,1-8.98562-1.939l-38.26114-59.31976A6.45336,6.45336,0,0,1,879.59864,536.87587Z",
      transform: "translate(-144.19918 -224.30788)"
    }
  ];

  const characterBodyRects = [
    {
      x: "896.89087", y: "280.79508", width: "1.99975", height: "10.77167",
      transform: "translate(352.41545 914.56571) rotate(-82.4768)"
    },
    {
      x: "930.59832", y: "285.24552", width: "1.99975", height: "10.77167",
      transform: "translate(377.29754 951.85076) rotate(-82.4768)"
    }
  ];

  pathElements.forEach(path => {
    const d = path.getAttribute('d');
    const transform = path.getAttribute('transform');

    const isCharacterBody = characterBodyPaths.some(bodyPath =>
      bodyPath.d === d && bodyPath.transform === transform
    );

    if (isCharacterBody) {
      path.setAttribute('fill', newColor);
    }
  });

  rectElements.forEach(rect => {
    const x = rect.getAttribute('x');
    const y = rect.getAttribute('y');
    const width = rect.getAttribute('width');
    const height = rect.getAttribute('height');
    const transform = rect.getAttribute('transform');

    const isCharacterBody = characterBodyRects.some(bodyRect =>
      bodyRect.x === x &&
      bodyRect.y === y &&
      bodyRect.width === width &&
      bodyRect.height === height &&
      bodyRect.transform === transform
    );

    if (isCharacterBody) {
      rect.setAttribute('fill', newColor);
    }
  });

  const serializer = new XMLSerializer();
  return serializer.serializeToString(xmlDoc);
}

// Example usage (assuming you have the SVG content as a string):
/*
const originalSvgContent = `<svg ...>...</svg>`; // Your SVG content here
const newSvgContent = changeCharacterBodyColor(originalSvgContent, "#ffffff");
console.log(newSvgContent);
*/
```

### **Parsing TOML with Jackson and Kotlin**

Pour parser des fichiers de configuration au format TOML, la bibliothèque Jackson avec le module `jackson-dataformat-toml` est une solution robuste. Elle permet de mapper directement un fichier TOML à des `data class` Kotlin.

*   **Exemple de fichier TOML (`config.toml`) :**

```toml
title = "TOML Example"

[owner]
name = "Tom Preston-Werner"
dob = 1979-05-27T07:32:00-08:00 # First class dates

[database]
server = "192.168.1.1"
ports = [ 8001, 8002, 8003 ]
connection_max = 5000
enabled = true
```

*   **`Data Classes` Kotlin correspondantes :**

Il faut définir des `data class` qui reflètent la structure du fichier TOML. Jackson s'occupera de la correspondance des noms de champs.

```kotlin
import com.fasterxml.jackson.annotation.JsonProperty

data class Config(
    val title: String,
    val owner: Owner,
    val database: Database
)

data class Owner(
    val name: String,
    val dob: java.time.OffsetDateTime
)

data class Database(
    val server: String,
    val ports: List<Int>,
    @JsonProperty("connection_max") // Utile si le nom du champ Kotlin diffère
    val connectionMax: Int,
    val enabled: Boolean
)
```

*   **Code de Parsing :**

L'utilisation de `TomlMapper` permet de lire le fichier et de le convertir en objet Kotlin.

```kotlin
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.io.File

fun parseTomlConfig(configFile: File): Config {
    val mapper = TomlMapper().apply {
        // Enregistre le module pour parser les dates/heures ISO-8601
        registerModule(JavaTimeModule())
    }
    return mapper.readValue(configFile, Config::class.java)
}

// --- Utilisation ---
// val myConfigFile = File("path/to/your/config.toml")
// val config = parseTomlConfig(myConfigFile)
// println("Server: ${config.database.server}")
// println("Owner: ${config.owner.name}")

```

Cette approche est simple, "type-safe" et bénéficie de la puissance de l'écosystème Jackson pour la gestion de formats de données.

### **Gestion des Dépendances avec Gradle Version Catalogs (`libs.versions.toml`)**

Gradle Version Catalogs, configurés via `gradle/libs.versions.toml`, permettent de centraliser la gestion des dépendances et de leurs versions. Pour rendre ces catalogues accessibles dans le module `buildSrc`, une configuration spécifique est nécessaire dans `buildSrc/settings.gradle.kts`.

*   **Configuration dans `buildSrc/settings.gradle.kts` :**

    En ajoutant le bloc `dependencyResolutionManagement` et `versionCatalogs` dans `buildSrc/settings.gradle.kts`, on déclare un catalogue de versions nommé `libs` qui pointe vers le fichier `../gradle/libs.versions.toml`.

    ```kotlin
    // buildSrc/settings.gradle.kts
    dependencyResolutionManagement {
        versionCatalogs {
            create("libs") {
                from(files("../gradle/libs.versions.toml"))
            }
        }
    }
    ```

*   **Accès au catalogue `libs` dans `buildSrc/build.gradle.kts` :**

    Une fois configuré, l'objet `libs` devient disponible dans `buildSrc/build.gradle.kts`. Cela permet de référencer les dépendances et les versions définies dans `libs.versions.toml` de manière "type-safe" et centralisée.

    ```kotlin
    // buildSrc/build.gradle.kts
    dependencies {
        // Exemple d'utilisation des dépendances définies dans libs.jbake
        implementation(libs.jbake)
        implementation(libs.slf4j.simple)
        // ...
    }

    // Accès aux versions (si nécessaire, bien que moins courant directement)
    // println(libs.versions.jbake.get())
    ```

    Cette approche améliore la maintenabilité et la cohérence des dépendances à travers le projet.

### **Guide Gradle `buildSrc` - Définition de Tâches en Kotlin DSL**

#### Introduction

Le dossier `buildSrc` est un projet Gradle spécial qui permet d'organiser la logique de build personnalisée de manière réutilisable. Il est automatiquement compilé et ses classes sont disponibles dans tous les scripts de build du projet principal.

#### Structure de base de `buildSrc`

```
buildSrc/
├── build.gradle.kts
└── src/
    └── main/
        └── kotlin/
            ├── plugins/
            ├── tasks/
            ├── extensions/
            └── conventions/
```

##### Configuration de `buildSrc/build.gradle.kts`

```kotlin
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    // Autres dépendances nécessaires
}
```

#### Stratégies de définition de tâches

##### 1. Plugin personnalisé avec tâches

**Cas d'usage** : Réutilisation maximale, logique complexe, distribution

```kotlin
// buildSrc/src/main/kotlin/plugins/DeploymentPlugin.kt
import org.gradle.api.Plugin
import org.gradle.api.Project

class DeploymentPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Enregistrement d'une tâche personnalisée
        project.tasks.register("deployToStaging", DeployTask::class.java) {
            group = "deployment"
            description = "Déploie l'application vers l'environnement de staging"
            environment.set("staging")
            targetUrl.set("https://staging.example.com")
        }
        
        project.tasks.register("deployToProduction", DeployTask::class.java) {
            group = "deployment"
            description = "Déploie l'application vers l'environnement de production"
            environment.set("production")
            targetUrl.set("https://prod.example.com")
        }
    }
}

// buildSrc/src/main/kotlin/tasks/DeployTask.kt
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class DeployTask : DefaultTask() {
    @get:Input
    abstract val environment: Property<String>
    
    @get:Input
    abstract val targetUrl: Property<String>
    
    @TaskAction
    fun deploy() {
        println("🚀 Déploiement vers ${environment.get()}")
        println("📡 URL cible: ${targetUrl.get()}")
        
        // Logique de déploiement
        project.exec {
            commandLine("kubectl", "apply", "-f", "k8s/${environment.get()}")
        }
    }
}
```

**Utilisation dans `build.gradle.kts`** :
```kotlin
plugins {
    id("deployment-plugin")
}
```

##### 2. Classes de tâches réutilisables

**Cas d'usage** : Tâches spécialisées, configuration flexible

```kotlin
// buildSrc/src/main/kotlin/tasks/DockerTask.kt
abstract class DockerTask : DefaultTask() {
    @get:Input
    abstract val imageName: Property<String>
    
    @get:Input
    abstract val dockerfile: Property<String>
    
    @get:Input
    abstract val buildContext: Property<String>
    
    init {
        group = "docker"
        dockerfile.convention("Dockerfile")
        buildContext.convention(".")
    }
    
    @TaskAction
    fun buildImage() {
        val image = imageName.get()
        val dockerfileValue = dockerfile.get()
        val context = buildContext.get()
        
        println("🐳 Construction de l'image Docker: $image")
        
        project.exec {
            commandLine("docker", "build", 
                "-f", dockerfileValue,
                "-t", image,
                context
            )
        }
    }
}

// buildSrc/src/main/kotlin/tasks/TestReportTask.kt
abstract class TestReportTask : DefaultTask() {
    @get:InputDirectory
    abstract val testResultsDir: DirectoryProperty
    
    @get:OutputFile
    abstract val reportFile: RegularFileProperty
    
    @TaskAction
    fun generateReport() {
        val resultsDir = testResultsDir.get().asFile
        val output = reportFile.get().asFile
        
        println("📊 Génération du rapport de tests")
        
        // Logique de génération de rapport
        output.writeText("""
            # Rapport de Tests
            
            Résultats: ${resultsDir.listFiles()?.size ?: 0} fichiers analysés
            Généré le: ${java.time.LocalDateTime.now()}
        """.trimIndent())
    }
}
```

**Utilisation dans `build.gradle.kts`** :
```kotlin
tasks.register<DockerTask>("buildAppImage") {
    imageName.set("myapp:${version}")
    dockerfile.set("docker/Dockerfile.app")
}

tasks.register<TestReportTask>("generateTestReport") {
    testResultsDir.set(layout.buildDirectory.dir("test-results"))
    reportFile.set(layout.buildDirectory.file("reports/custom-test-report.md"))
    dependsOn("test")
}
```

##### 3. Extensions de projet avec DSL personnalisé

**Cas d'usage** : Configuration fluide, API métier

```kotlin
// buildSrc/src/main/kotlin/extensions/MicroserviceExtension.kt
abstract class MicroserviceExtension {
    abstract val serviceName: Property<String>
    abstract val version: Property<String>
    abstract val port: Property<Int>
    
    fun createTasks(project: Project) {
        project.tasks.register("startService") {
            group = "microservice"
            description = "Démarre le microservice ${serviceName.get()}"
            
            doLast {
                println("🚀 Démarrage du service ${serviceName.get()} sur le port ${port.get()}")
                project.exec {
                    commandLine("java", "-jar", 
                        "build/libs/${serviceName.get()}-${version.get()}.jar",
                        "--server.port=${port.get()}"
                    )
                }
            }
        }
        
        project.tasks.register("healthCheck") {
            group = "microservice"
            description = "Vérifie la santé du service"
            
            doLast {
                val url = "http://localhost:${port.get()}/actuator/health"
                println("🏥 Vérification de santé: $url")
                // Logique de health check
            }
        }
    }
}

// buildSrc/src/main/kotlin/plugins/MicroservicePlugin.kt
class MicroservicePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("microservice", MicroserviceExtension::class.java)
        
        project.afterEvaluate {
            extension.createTasks(project)
        }
    }
}
```

**Utilisation dans `build.gradle.kts`** :
```kotlin
plugins {
    id("microservice-plugin")
}

microservice {
    serviceName.set("user-service")
    version.set("1.0.0")
    port.set(8080)
}
```

##### 4. Convention plugins

**Cas d'usage** : Standardisation, configuration par convention

```kotlin
// buildSrc/src/main/kotlin/conventions/spring-boot-conventions.gradle.kts
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar {
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.register("createDockerfile") {
    group = "docker"
    description = "Crée un Dockerfile pour l'application Spring Boot"
    
    val outputFile = layout.buildDirectory.file("docker/Dockerfile")
    outputs.file(outputFile)
    
    doLast {
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("""
                FROM openjdk:17-jre-slim
                
                WORKDIR /app
                COPY build/libs/*.jar app.jar
                
                EXPOSE 8080
                ENTRYPOINT ["java", "-jar", "app.jar"]
            """.trimIndent())
        }
        println("🐳 Dockerfile créé: ${outputFile.get()}")
    }
}

tasks.register("packageForDocker") {
    group = "docker"
    description = "Package l'application pour Docker"
    dependsOn("bootJar", "createDockerfile")
}
```

**Utilisation dans `build.gradle.kts`** :
```kotlin
plugins {
    id("spring-boot-conventions")
}
```

#### Patterns avancés et bonnes pratiques

##### Tâches avec configuration complexe

```kotlin
// buildSrc/src/main/kotlin/tasks/DatabaseMigrationTask.kt
abstract class DatabaseMigrationTask : DefaultTask() {
    @get:Input
    abstract val databaseUrl: Property<String>
    
    @get:Input
    abstract val username: Property<String>
    
    @get:Input
    abstract val password: Property<String>
    
    @get:InputDirectory
    abstract val migrationsDir: DirectoryProperty
    
    @get:Input
    abstract val action: Property<MigrationAction>
    
    enum class MigrationAction { MIGRATE, ROLLBACK, STATUS }
    
    @TaskAction
    fun executeMigration() {
        when (action.get()) {
            MigrationAction.MIGRATE -> migrate()
            MigrationAction.ROLLBACK -> rollback()
            MigrationAction.STATUS -> status()
        }
    }
    
    private fun migrate() {
        println("📊 Migration de la base de données")
        // Logique de migration
    }
    
    private fun rollback() {
        println("⏪ Rollback de la base de données")
        // Logique de rollback
    }
    
    private fun status() {
        println("ℹ️ Statut de la base de données")
        // Logique de statut
    }
}
```

##### Tâches avec dépendances et orchestration

```kotlin
// buildSrc/src/main/kotlin/plugins/ContinuousDeploymentPlugin.kt
class ContinuousDeploymentPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Tâche de build complet
        project.tasks.register("fullBuild") {
            group = "ci-cd"
            description = "Build complet avec tests et qualité"
            dependsOn("clean", "test", "detekt", "build")
        }
        
        // Tâche de déploiement avec validation
        project.tasks.register("deployWithValidation") {
            group = "ci-cd"
            description = "Déploie avec validation complète"
            dependsOn("fullBuild")
            
            doLast {
                // Validation pré-déploiement
                validateEnvironment()
                deploy()
                validateDeployment()
            }
        }
        
        // Tâche de rollback d'urgence
        project.tasks.register("emergencyRollback") {
            group = "ci-cd"
            description = "Rollback d'urgence"
            
            doLast {
                println("🚨 Rollback d'urgence en cours...")
                // Logique de rollback
            }
        }
    }
    
    private fun validateEnvironment() {
        println("✅ Validation de l'environnement")
    }
    
    private fun deploy() {
        println("🚀 Déploiement en cours")
    }
    
    private fun validateDeployment() {
        println("🔍 Validation du déploiement")
    }
}
```

#### Configuration et utilisation

##### Enregistrement des plugins dans `buildSrc`

```kotlin
// buildSrc/src/main/kotlin/plugins/Plugins.kt
object Plugins {
    const val DEPLOYMENT = "deployment-plugin"
    const val MICROSERVICE = "microservice-plugin"
    const val CI_CD = "continuous-deployment-plugin"
}
```

##### Déclaration dans le projet principal

```kotlin
// build.gradle.kts
plugins {
    id("deployment-plugin")
    id("microservice-plugin")
    id("continuous-deployment-plugin")
}

// Configuration des tâches
tasks.named("deployToProduction") {
    mustRunAfter("test")
}

// Création de tâches ad-hoc utilisant les classes de buildSrc
tasks.register<DockerTask>("buildProductionImage") {
    imageName.set("myapp:production")
    dockerfile.set("Dockerfile.prod")
}
```

#### Conseils pour l'assistant agentique

##### Patterns de workflow automatisés

1. **Build → Test → Deploy** : Utilisez les dépendances de tâches
2. **Validation en pipeline** : Créez des tâches de validation entre les étapes
3. **Rollback automatique** : Implémentez des tâches de rollback en cas d'échec
4. **Monitoring** : Ajoutez des tâches de health check post-déploiement

##### Template de tâche générique

```kotlin
abstract class WorkflowTask : DefaultTask() {
    @get:Input
    abstract val environment: Property<String>
    
    @get:Input  
    abstract val dryRun: Property<Boolean>
    
    init {
        dryRun.convention(false)
    }
    
    @TaskAction
    fun execute() {
        if (dryRun.get()) {
            println("🔍 Mode dry-run activé")
            simulateExecution()
        } else {
            performExecution()
        }
    }
    
    abstract fun simulateExecution()
    abstract fun performExecution()
}
```

##### Variables d'environnement et configuration

```kotlin
// Configuration centralisée
object Config {
    val environment = System.getenv("ENVIRONMENT") ?: "development"
    val version = System.getenv("VERSION") ?: "SNAPSHOT"
    val isCI = System.getenv("CI") != null
}
```

Ce guide vous donne une base complète pour automatiser vos workflows Gradle avec `buildSrc` et les tâches personnalisées en Kotlin DSL.

### **Refactorisation du Build Gradle : Migration de la Logique de Publication vers `buildSrc`**

La maintenabilité d'un projet passe aussi par la propreté de ses scripts de build. Une logique de build monolithique dans `build.gradle.kts`, surtout avec de la duplication de code, devient rapidement difficile à gérer et à tester. La migration de cette logique vers le répertoire `buildSrc` est une pratique exemplaire de Gradle qui offre plusieurs avantages clés.

*   **Principe de la Migration :**
    Le but est de déplacer le code impératif (les fonctions, les classes) du script `build.gradle.kts` vers des fichiers source Kotlin (`.kt`) dans le répertoire `buildSrc`. Le script de build principal redevient alors ce qu'il doit être : un fichier déclaratif qui applique des plugins et configure des tâches.

*   **Avantages de l'utilisation de `buildSrc` :**
    1.  **Meilleure Organisation et Lisibilité :** Le code est structuré en packages et en fichiers, suivant les conventions Kotlin, ce qui le rend plus facile à naviguer et à comprendre.
    2.  **Réutilisabilité :** La logique encapsulée dans `buildSrc` peut être facilement appelée par différentes tâches ou même partagée entre plusieurs sous-projets.
    3.  **Amélioration des Performances :** Gradle compile et met en cache le code de `buildSrc`. Il ne sera pas recompilé à chaque exécution du build, sauf si ses sources changent, ce qui peut accélérer les builds.
    4.  **Testabilité :** Le code dans `buildSrc` est du code Kotlin standard. Il peut donc être testé unitairement avec des frameworks comme JUnit, ce qui permet de valider la logique de build et de prévenir les régressions, une chose quasi impossible à faire proprement sur un script monolithique.

*   **Méthodologie Appliquée (Synthèse de l'US-REFACTO-BUILD-01) :**
    La migration a été menée de manière itérative et sécurisée :
    1.  **Refactorisation sur place :** D'abord, le code dupliqué dans `build.gradle.kts` a été éliminé en créant des fonctions génériques.
    2.  **Validation continue :** Chaque petite étape de cette refactorisation a été validée par une commande `./gradlew check` pour s'assurer que le build restait stable.
    3.  **Migration finale :** Une fois la logique propre et centralisée, elle a été simplement déplacée vers `buildSrc/src/main/kotlin/com/cheroliv/logic/Publishing.kt`.
    4.  **Finalisation :** Les `import` nécessaires ont été ajoutés dans `build.gradle.kts` pour qu'il puisse utiliser le code désormais externalisé.

Cette approche a permis de transformer un script de build complexe en une solution propre, performante et maintenable, prête pour de futures évolutions et des tests automatisés.

### Stabilisation de la Suite de Tests : Analyse des Problèmes et Couverture Actuelle

Cette section détaille les problèmes techniques résolus pour fiabiliser la suite de tests et décrit la couverture de test effective qui en résulte.

#### 1. Le Problème Bloquant : Un Conflit de Processus et de Contexte

Le blocage principal n'était pas un simple bug, mais un **conflit fondamental** entre la manière dont les tests étaient exécutés et ce qu'ils demandaient au système de faire. Il y avait deux problèmes majeurs, un dans chaque suite de tests :

##### a) Le Test "Suicidaire" du Projet Racine

*   **Le Symptôme :** Le test de la tâche `serve` se lançait, le serveur démarrait, mais tout le processus Gradle se terminait brutalement (avec le code `143 SIGTERM`) au moment de lancer `stopServe`. Il n'y avait pas de message d'erreur, juste un arrêt net.
*   **La Cause Racine :** La tâche `stopServe` était définie dans `build.gradle.kts` avec la commande `killall java`. C'était une approche "marteau-pilon" : elle demandait au système de tuer **tous les processus Java** lancés par l'utilisateur courant.
*   **Le Conflit :** Le processus Gradle qui exécute les tests **est lui-même un processus Java**. Donc, lorsque notre test, depuis ce processus Gradle, a lancé la commande `./gradlew stopServe`, il a enclenché une chaîne qui a abouti à sa propre destruction. Le test se "suicidait" involontairement avant d'avoir pu terminer et rapporter son succès.
*   **La Solution :** Nous avons remplacé cette commande destructrice par une **logique chirurgicale**. La tâche `serve` écrit maintenant le numéro d'identification (PID) du serveur JBake dans un fichier. La tâche `stopServe` lit ce fichier et ne tue que ce processus spécifique (`kill <PID>`), laissant le processus de test Gradle intact.

##### b) Le Test "Perdu" dans `buildSrc`

*   **Le Symptôme Initial :** Le test dans `buildSrc` échouait immédiatement avec une erreur `IOException: Aucun fichier ou dossier de ce nom`, en essayant d'exécuter `./gradlew`.
*   **La Cause Racine :** Les tests de `buildSrc` s'exécutent dans leur propre contexte, avec le répertoire `buildSrc` comme répertoire de travail. Le code du test cherchait le script `gradlew` à cet endroit, mais ce script n'existe qu'à la racine du projet. Le test était "perdu" et ne trouvait pas son outil principal.
*   **Le Symptôme Secondaire :** Une fois le chemin corrigé, le test échouait à nouveau, cette fois sur une assertion de contenu. Le contenu servi par le serveur ne correspondait pas au contenu du fichier généré.
*   **La Cause Racine :** La commande `curl`, utilisée pour récupérer le contenu du serveur, affiche par défaut une barre de progression et des statistiques de transfert. Ces informations étaient capturées avec le code HTML, "polluant" ainsi le résultat et le rendant différent du fichier HTML pur.
*   **La Solution :** Nous avons corrigé le chemin pour qu'il remonte correctement au `gradlew` de la racine. Puis, nous avons ajouté l'option `-s` (silencieux) à la commande `curl` pour qu'elle ne retourne que le contenu brut, permettant une comparaison fiable.

---

#### 2. La Couverture de Test Efficace que Nous Avons Maintenant

Grâce à la résolution de ces problèmes, nous disposons maintenant d'une couverture de test qui nous donne une confiance réelle sur plusieurs aspects critiques du projet :

##### a) Validation Complète du Cycle de Vie du Serveur de Développement
Le test dans `src/test/kotlin/site/ProjectTests.kt` valide de bout en bout le cycle de vie de nos tâches de serveur :
*   **Démarrage Fiable :** Il prouve que la tâche `serve` est capable de démarrer le serveur JBake et que celui-ci devient accessible sur le réseau local (`localhost:8820`).
*   **Arrêt Propre et Ciblé :** Il prouve que la tâche `stopServe` arrête **uniquement** le serveur JBake concerné sans aucun effet de bord sur les autres processus (comme le build Gradle).
*   **Disponibilité du Service :** Il confirme que le serveur, une fois démarré, répond correctement aux requêtes HTTP.

=> **Confiance acquise :** Nous pouvons modifier les tâches `serve` et `stopServe` à l'avenir et être sûrs de ne pas introduire de régression sur leur comportement de base.

##### b) Validation de l'Intégrité du Contenu ("Ce qui est généré est ce qui est servi")
Le test dans `buildSrc/src/test/kotlin/site/core/ServerContentTest.kt` agit comme un test d'intégration de haut niveau pour notre processus de build et de service :
*   **Orchestration des Tâches :** Il prouve que la logique dans `buildSrc` peut correctement appeler les tâches du projet racine (`bake`, `serve`, `stopServe`).
*   **Cohérence du Contenu :** C'est le point le plus important : il valide que le fichier `index.html` généré par la tâche `bake` est **exactement le même** que celui qui est servi par la tâche `serve`.

=> **Confiance acquise :** Nous sommes sûrs qu'il n'y a pas de corruption ou de modification inattendue du contenu entre le moment où le site est généré et le moment où il est servi en local.

##### c) Validation de l'Infrastructure de Build (`buildSrc`)
Le simple fait que les tests de `buildSrc` s'exécutent avec succès valide plusieurs points fondamentaux :
*   **Configuration Correcte :** Le module `buildSrc` est correctement configuré pour compiler son propre code et exécuter ses propres tests.
*   **Accès à l'Environnement :** La logique de test dans `buildSrc` est capable d'interagir correctement avec l'environnement du projet racine (trouver et exécuter `gradlew`).

=> **Confiance acquise :** Nous avons une base saine pour ajouter à l'avenir des tests plus complexes à notre logique de build (par exemple, pour tester la logique de publication `jgit`).

### **Développement de Plugins Gradle : Approche TDD et Stabilité contre "Vibe Coding"**

Le développement du plugin `site-baker` a été abordé non pas comme une simple implémentation de fonctionnalités, mais comme une opportunité de définir une approche de développement robuste et maintenable. Cette démarche s'oppose consciemment au "vibe coding" — une méthode de développement organique où le code est ajouté de manière intuitive jusqu'à ce que le système devienne complexe et difficile à stabiliser.

La stratégie adoptée repose sur plusieurs piliers du software craftsmanship :

1.  **Fondations Saines et Standardisées :** Le projet a été initié avec `gradle init`, garantissant une structure de base saine et conventionnelle, plutôt que de partir d'un existant complexe.

2.  **Développement Guidé par les Tests (TDD) :** Chaque nouvelle fonctionnalité est développée en suivant le cycle TDD :
    *   **Test d'abord :** Définir le comportement attendu via un test (souvent fonctionnel) qui échoue initialement.
    *   **Implémentation minimale :** Écrire le strict minimum de code pour que le test passe.
    *   **Refactorisation :** Améliorer la qualité du code (clarté, performance) tout en s'assurant que les tests continuent de passer.

3.  **Définition du Contrat d'Interface (DSL) en Premier :** Avant d'implémenter la logique interne, l'interface publique du plugin (son DSL) a été définie. Cela force à réfléchir à l'expérience de l'utilisateur final et crée un "contrat" clair que l'implémentation doit respecter.

4.  **Changements Atomiques et Incrémentaux :** Plutôt que de réaliser de grands changements complexes, le développement est découpé en petites étapes logiques et atomiques. Chaque étape est validée par des tests avant de passer à la suivante. Par exemple :
    *   **Étape 1 :** Mettre en place le DSL et prouver qu'il est lisible par une tâche simple.
    *   **Étape 2 :** Ajouter la lecture du fichier de configuration spécifié dans le DSL.
    *   **Étape 3 :** Implémenter la logique métier qui utilise cette configuration.

Cette approche méthodique, bien que potentiellement plus lente au démarrage, garantit une base de code stable, testable et évolutive. Elle transforme l'incertitude en une série d'étapes maîtrisées, menant à un logiciel de bien meilleure qualité.

### **Épopée 13 : Mise en Place de l'Infrastructure RAG**

#### Activation de l'extension `pgvector` sur Supabase

L'extension `pgvector` est indispensable pour le projet RAG. Elle ajoute à PostgreSQL la capacité de stocker des embeddings vectoriels et d'exécuter des recherches de similarité, ce qui est le cœur du mécanisme de recherche de contexte.

Pour l'activer sur votre instance Supabase :

1.  Accédez à votre tableau de bord Supabase.
2.  Naviguez vers la section **Database**.
3.  Dans le menu de gauche, sélectionnez **Extensions**.
4.  Utilisez la barre de recherche pour trouver `vector`.
5.  Cliquez sur **Enable** pour activer l'extension.

### Vérification Locale du Site Statique avec `jbake.sh`

*   **Contexte :** Lors de la modification de la configuration de la génération du site (ex: `site/jbake.properties`), il est crucial de pouvoir vérifier l'impact des changements rapidement, sans avoir à déployer sur un environnement distant. Cette méthode de vérification locale fournit une boucle de feedback immédiate.
*   **Principe :** La méthode consiste à exécuter le script de build local (`jbake.sh`), puis à inspecter les fichiers générés dans le répertoire `build/jbake` pour valider deux points essentiels :
    1.  **Existence des Artefacts :** Les fichiers attendus (notamment les images générées comme les diagrammes PlantUML) sont-ils physiquement présents au bon endroit ?
    2.  **Validité des Liens :** Le code HTML généré contient-il des liens (ex: `src` pour une balise `<img>`) qui pointent correctement vers l'emplacement de ces artefacts ?
*   **Méthodologie de Vérification Détaillée pour un LLM :**
    Pour garantir une vérification fiable, il faut suivre un processus en trois étapes : Nettoyer, Construire, et Inspecter.

    1.  **Nettoyer (`clean`) :** Toujours commencer par supprimer le répertoire de build précédent (`build/jbake`) pour s'assurer qu'aucun ancien fichier ne vienne fausser l'analyse.
    2.  **Construire (`build`) :** Exécuter le script `./jbake.sh`. Il est **critique** de ne pas masquer la sortie d'erreur (`stderr`). Si le script échoue (par exemple, à cause d'une mauvaise configuration), le processus de vérification doit s'arrêter immédiatement.
    3.  **Inspecter (`inspect`) :** Une fois le build terminé avec succès, inspecter les résultats :
        *   **Vérifier la présence des fichiers :** Utiliser une commande comme `find` pour localiser les fichiers générés. Par exemple, pour trouver tous les diagrammes SVG : `find build/jbake -name "*.svg"`.
        *   **Vérifier le contenu HTML :** Utiliser une commande comme `grep` pour extraire les balises pertinentes du HTML généré et vérifier leurs attributs. Par exemple, pour inspecter les liens d'image dans un article : `grep 'src=".*\\.svg"' build/jbake/blog/2025/0094_tor_browser_install_post.html`.

*   **Commande de Vérification Robuste (Exemple Concret) :**
    La commande suivante enchaîne ces étapes de manière sécurisée. Elle s'arrêtera si l'une des étapes échoue.

    ```bash
    # 1. Nettoie le répertoire de build
    rm -rf build/jbake && \
    # 2. Exécute le build et s'arrête en cas d'erreur
    ./jbake.sh && \
    # 3. Liste les fichiers SVG générés pour vérifier leur existence et leur emplacement
    echo "--- Fichiers SVG trouvés ---" && \
    find build/jbake -name "*.svg" && \
    # 4. Inspecte le lien dans le fichier HTML pour vérifier sa validité
    echo "--- Balise <img> dans le HTML ---" && \
    grep 'src=".*\\.svg"' build/jbake/blog/2025/0094_tor_browser_install_post.html
    ```

    *   **Analyse de la commande :**
        *   `rm -rf build/jbake` : Supprime l'ancien site.
        *   `./jbake.sh` : Génère le nouveau site.
        *   `&&` : C'est l'opérateur "ET logique". Il garantit que la commande suivante ne s'exécute **que si** la commande précédente a réussi (c'est-à-dire, a retourné un code de sortie de 0). C'est la clé pour éviter d'inspecter des résultats invalides.
        *   `find ...` et `grep ...` : Ces commandes permettent d'inspecter les résultats. Si elles ne trouvent rien, la sortie sera vide, indiquant un problème.

    Cette méthode structurée permet de diagnostiquer précisément les problèmes de génération de site en local, en s'assurant que ce qui est généré est bien ce qui est attendu.

### Configuration AsciiDoc pour la Génération d'Images (`jbake.properties`)

*   **Contexte :** La génération de diagrammes PlantUML (ou autres) directement depuis les fichiers AsciiDoc est une fonctionnalité puissante gérée par `asciidoctor-diagram`. Cependant, sa configuration via `jbake.properties` peut être source d'erreurs si l'interaction entre les différents attributs n'est pas bien comprise.
*   **Les Attributs Clés :**
    La configuration se joue principalement sur deux attributs AsciiDoc :
    1.  `imagesoutdir` : Cet attribut indique à `asciidoctor-diagram` **où générer le fichier image physique**. Le chemin est relatif au répertoire de destination final du fichier HTML.
    2.  `imagesdir` : Cet attribut indique au processeur AsciiDoctor **quel préfixe de chemin ajouter à l'attribut `src` de la balise `<img>`** dans le fichier HTML final.

*   **Le Conflit et les Erreurs Rencontrées :**
    Notre problème venait d'une mauvaise synchronisation entre ces deux attributs.
    *   **Tentative 1 (Centralisation) :** `imagesoutdir=images/diagrams` et `imagesdir=/images/diagrams`.
        *   **Résultat Attendu :** Que les images soient générées dans `build/jbake/images/diagrams` et que le lien HTML soit `<img src="/images/diagrams/diag.svg">`.
        *   **Résultat Réel :** Le processus de build échouait, probablement car `jbake` et `asciidoctor-diagram` n'arrivaient pas à résoudre correctement ce chemin absolu lors de la génération.
    *   **Tentative 2 (Chemin relatif complexe) :** `imagesoutdir=.` et `imagesdir={docname}`.
        *   **Résultat Attendu :** Que l'image soit générée dans un sous-dossier (`build/jbake/blog/2025/mon-article/`) et que le lien HTML pointe vers ce sous-dossier.
        *   **Résultat Réel :** Le lien HTML était incorrect (`<img src="mon-article.html/diag.svg">`), car la variable `{docname}` n'était pas interprétée comme un simple nom de dossier.

*   **La Solution : La Simplicité du Comportement par Défaut**
    La solution la plus robuste et qui a finalement fonctionné a été de **ne spécifier aucun de ces deux attributs**.

    *   **Configuration Finale dans `jbake.properties` :**
        ```properties
        asciidoctor.attributes=sourceDir=site,source-highlighter=highlight.js,icons=font
        ```
    *   **Pourquoi ça marche ?**
        En l'absence de configuration spécifique, `asciidoctor-diagram` adopte son comportement par défaut, qui est le plus logique et le plus simple :
        1.  **`imagesoutdir` (implicite) :** Il génère le fichier image (`.svg`) **exactement dans le même répertoire** que le fichier HTML qui le contient (ex: `build/jbake/blog/2025/`).
        2.  **`imagesdir` (implicite) :** Le préfixe du lien est vide. Le chemin dans le HTML est donc une simple référence relative : `<img src="diag.svg">`.

    Cette configuration par défaut assure un alignement parfait entre l'emplacement physique de l'image et le lien qui y pointe, résolvant ainsi le problème de manière élégante et sans maintenance.

### **Tests Unitaires de Plugins Gradle : De la Difficulté de `ProjectBuilder` au Mocking Pur avec Mockito**

*   **Contexte :** Lors de l'écriture des tests unitaires pour le plugin `com.cheroliv.bakery`, l'objectif était de valider la création de la configuration `jbakeRuntime` et l'ajout de ses dépendances.
*   **La Problématique Initiale : L'Impasse `ProjectBuilder`**
    *   L'approche initiale utilisait `ProjectBuilder.builder().build()` pour créer une instance de `Project`. Bien que simple, cette méthode crée un objet `Project` "réel" mais vide, dépourvu des extensions et configurations nécessaires au plugin, notamment le `VersionCatalogsExtension` (pour l'accès à `libs`).
    *   Cela entraînait des `NullPointerException` dès que le code du plugin tentait d'accéder à ces extensions.
    *   Les tentatives pour "patcher" manuellement l'objet `Project` en y ajoutant des mocks se sont avérées complexes, verbeuses et fragiles, menant à une impasse technique.

*   **La Solution : Le Mocking Pur et la Vérification de Comportement**
    Le déblocage est venu d'un changement de paradigme : abandonner l'idée de tester un *état* (les propriétés d'un objet `Project` réel) pour se concentrer sur la vérification d'un *comportement* (s'assurer que notre plugin appelle les bonnes méthodes sur l'objet `Project`).

    1.  **Mock Intégral de `Project` :** Au lieu de `ProjectBuilder`, nous utilisons `mock<Project>()` de `mockito-kotlin` pour créer un `Project` entièrement factice.
    2.  **Simulation de la Chaîne d'Appels :** Nous configurons ce mock pour qu'il réponde précisément aux appels que notre plugin effectue : `project.getExtensions()`, `project.getConfigurations()`, `project.getDependencies()`, etc. Chaque appel retourne un autre mock, simulant ainsi toute la chaîne d'interaction.
    3.  **Vérification avec `verify` :** Les tests n'utilisent plus `assertThat` pour inspecter l'état du projet. À la place, ils utilisent `Mockito.verify` pour confirmer que les méthodes attendues ont bien été appelées avec les bons arguments (ex: `verify(project.configurations).create("jbakeRuntime", ...)`).
    4.  **Capture de Logique avec `argumentCaptor` :** Pour valider la logique passée dans des lambdas (comme la configuration de la description), un `argumentCaptor` est utilisé. Il capture l'action (`Action<Configuration>`) passée à la méthode `create`, nous permettant de l'exécuter et de vérifier qu'elle se comporte comme prévu sur un mock de `Configuration`.

*   **Leçons Apprises et Bonnes Pratiques :**
    *   **`ProjectBuilder` est pour les tests d'intégration :** `ProjectBuilder` est plus adapté aux tests fonctionnels ou d'intégration qui nécessitent un véritable cycle de vie de projet et une interaction avec le système de fichiers.
    *   **Le Mocking Pur est pour les tests unitaires :** Pour tester la logique d'un plugin en isolation, le mocking intégral est plus rapide, plus robuste et moins sujet aux changements internes de l'API de Gradle.
    *   **Tester le Comportement, pas l'État :** Pour les plugins, il est souvent plus pertinent de vérifier que le plugin a demandé à Gradle de faire les bonnes choses (comportement) plutôt que de vérifier l'état final de l'objet `Project` (état).
    *   **`argumentCaptor` est essentiel :** C'est l'outil clé pour tester le code de configuration passé dans des closures, un pattern très courant dans les DSL Gradle.
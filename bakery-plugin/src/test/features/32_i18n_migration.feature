@cucumber @bakery @i18n @migration
Feature: Migration i18n — Tâche migrateToI18n — BKY-I18N-MIG US-2+3

  La tâche `migrateToI18n` scanne les templates, extrait le texte
  hardcodé, génère `messages_{lang}.properties` et remplace le texte
  par `#{message.key}`. Cascade CLI > DSL > defaults.
  La logique métier est couverte par 53 tests unitaires (I18nMigrationServiceTest
  + MigrateToI18nTaskTest). Les scénarios Cucumber valident l'intégration Gradle.

  Background:
    Given a new Bakery project with site fully configured

  @task-registered
  Scenario: La tâche migrateToI18n est enregistrée dans le pipeline full
    When I am executing the task 'tasks'
    Then the build should succeed
    And task "migrateToI18n" should be registered

  @dry-run-preview
  Scenario: Dry run preview — la tâche s'exécute sans erreur
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/test-site" and languages "en,ar" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'tasks'
    Then the build should succeed

  @migration-en-ar
  Scenario: Migration avec langues EN+AR configurée via DSL
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/test-site" and languages "en,ar" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'tasks'
    Then the build should succeed

  @migration-cli-options
  Scenario: Options CLI i18nSite et i18nLangs acceptées
    Given a new Bakery project with site fully configured
    When I am executing the task 'migrateToI18n' with arguments '--i18nSite=/tmp/test-site --i18nLangs=en,es --i18nDryRun=true'
    Then the build should succeed

  @migration-no-templates
  Scenario: Site sans templates — la tâche ne crash pas
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/empty-site" and languages "en" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'tasks'
    Then the build should succeed

  @migration-dryrun-extract
  Scenario: Dry run extrait les clés sans modifier les fichiers
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/dryrun-site" and languages "en,ar" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'tasks'
    Then the build should succeed

  @migration-real-write
  Scenario: Migration réelle génère messages.properties et modifie templates
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/real-site" and languages "en,ar" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'tasks'
    Then the build should succeed

  @migration-already-i18n
  Scenario: Site déjà i18n — aucun changement
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/already-i18n" and languages "en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'tasks'
    Then the build should succeed

  @migration-no-hardcoded
  Scenario: Site sans texte hardcodé — zéro extraction
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/no-text" and languages "en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'tasks'
    Then the build should succeed

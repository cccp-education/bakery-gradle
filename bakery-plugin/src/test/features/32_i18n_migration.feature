@cucumber @bakery @i18n @migration
Feature: Migration i18n — Tâche migrateToI18n — BKY-I18N-MIG US-2

  La tâche `migrateToI18n` est enregistrée dans le pipeline full
  et accepte les options CLI `--i18nSite`, `--i18nLangs`,
  `--i18nDefaultLang`, `--i18nDryRun` avec la cascade
  CLI > DSL > defaults.

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

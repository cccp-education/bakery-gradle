#cucumber @bakery @i18n @migration
Feature: Migration i18n — BKY-I18N-MIG US-1

  Le DSL `bakery { i18nMigration { ... } }` est correctement wire
  dans BakeryExtension et accepte les parametres siteDir, languages,
  defaultLanguage, dryRun sans erreur de compilation/configuration.

  Scenario: DSL i18nMigration valide ne cause pas d'erreur de configuration
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/test-site" and languages "en,ar" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'tasks'
    Then the build should succeed

  Scenario: DSL i18nMigration avec siteDir vide ne cause pas d'erreur de configuration
    Given a new Bakery project with i18n migration intention configured with siteDir "" and languages "en" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'tasks'
    Then the build should succeed

  Scenario: DSL i18nMigration avec dryRun false
    Given a new Bakery project with i18n migration intention configured with siteDir "/tmp/test-site" and languages "en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'tasks'
    Then the build should succeed

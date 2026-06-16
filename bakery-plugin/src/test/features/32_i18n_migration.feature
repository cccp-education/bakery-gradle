@cucumber @bakery @i18n @migration
Feature: Migration i18n — Tâche migrateToI18n — BKY-I18N-MIG US-5

  La tâche `migrateToI18n` scanne les templates, extrait le texte
  hardcodé, génère `messages_{lang}.properties` et remplace le texte
  par `#{message.key}`. Cascade CLI > DSL > defaults.
  La logique métier est couverte par 53 tests unitaires (I18nMigrationServiceTest
  + MigrateToI18nTaskTest). Les scénarios Cucumber valident l'intégration Gradle
  avec assertions post-exécution.

  Background:
    Given a new Bakery project with site fully configured

  @task-registered
  Scenario: La tâche migrateToI18n est enregistrée dans le pipeline full
    When I am executing the task 'tasks'
    Then the build should succeed
    And task "migrateToI18n" should be registered

  @dry-run-preview
  Scenario: Dry run preview — extrait les clés sans modifier les fichiers
    Given a real site directory "dryrun-site" with templates containing hardcoded French text
    And i18n migration DSL configured with siteDir pointing to the real site and languages "en,ar" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "Clés extraites"
    And the output should contain "DRY-RUN"
    And no messages_en.properties file should exist in the templates directory
    And the templates should NOT be modified

  @migration-real-write
  Scenario: Migration réelle génère messages.properties et modifie templates
    Given a real site directory "real-site" with templates containing hardcoded French text
    And i18n migration DSL configured with siteDir pointing to the real site and languages "en,ar" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "Clés extraites"
    And the output should contain "Fichiers générés"
    And messages_en.properties should exist in the templates directory
    And messages_ar.properties should exist in the templates directory
    And the templates should contain th:text message keys
    And site.language=fr should be injected in jbake.properties

  @migration-no-templates
  Scenario: Site sans templates — la tâche avertit sans crasher
    Given a real site directory "empty-site" with NO templates directory
    And i18n migration DSL configured with siteDir pointing to the real site and languages "en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "Aucun répertoire templates"

  @migration-already-i18n
  Scenario: Site déjà i18n — aucun changement
    Given a real site directory "already-i18n" with templates already using th:text message keys
    And i18n migration DSL configured with siteDir pointing to the real site and languages "en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "Clés extraites : 0"
    And no new messages files should be created

  @migration-no-hardcoded
  Scenario: Site sans texte hardcodé — zéro extraction
    Given a real site directory "no-text" with templates containing only empty divs
    And i18n migration DSL configured with siteDir pointing to the real site and languages "en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "Clés extraites : 0"

  @migration-cli-options
  Scenario: Options CLI i18nSite et i18nLangs acceptées avec assertions
    Given a real site directory "cli-site" with templates containing hardcoded French text
    When I am executing the task 'migrateToI18n' with CLI options for the real site and languages "en,es" and dryRun "true"
    Then the build should succeed
    And the output should contain "Clés extraites"
    And the output should contain "DRY-RUN"

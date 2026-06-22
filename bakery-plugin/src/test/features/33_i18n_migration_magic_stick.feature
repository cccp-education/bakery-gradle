@cucumber @bakery @i18n @migration @magic-stick
Feature: Migration i18n réelle — Fixture magic-stick — BKY-I18N-PROD-MIG

  Valider que `migrateToI18n` en mode dryRun=false fonctionne sur une copie
  réelle des templates magic-stick sans jamais toucher au site de production.
  Cette feature est le test de non-régression avant migration en production.

  Background:
    Given a new Bakery project with site fully configured

  @magic-stick-real-migration
  Scenario: Migration réelle de la fixture magic-stick génère les messages et modifie les templates
    Given a real site directory "magic-stick-fixture" copied from test resources "i18n-fixtures/magic-stick/jbake"
    And i18n migration DSL configured with siteDir pointing to the real site and languages "fr,en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "Clés extraites"
    And the output should contain "Fichiers générés"
    And messages_fr.properties should exist in the templates directory
    And messages_en.properties should exist in the templates directory
    And the templates should contain th:text message keys
    And site.language=fr should be injected in jbake.properties

  @magic-stick-dry-run
  Scenario: Dry run sur la fixture magic-stick ne modifie rien
    Given a real site directory "magic-stick-fixture-dryrun" copied from test resources "i18n-fixtures/magic-stick/jbake"
    And i18n migration DSL configured with siteDir pointing to the real site and languages "fr,en" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "DRY-RUN"
    And no messages_en.properties file should exist in the templates directory
    And the templates should NOT be modified

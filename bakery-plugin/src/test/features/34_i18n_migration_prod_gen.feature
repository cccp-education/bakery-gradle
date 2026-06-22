@cucumber @bakery @i18n @migration @prod-gen
Feature: Migration i18n généralisée — Fixtures cccp-education et cheroliv-com — BKY-I18N-PROD-GEN

  Valider que `migrateToI18n` fonctionne sur des copies réelles des fixtures
  cccp-education (vitrine OSS) et cheroliv-com (blog/portfolio) sans toucher aux
  sites de production. Cette feature couvre l'US-GEN-3 du backlog.

  Background:
    Given a new Bakery project with site fully configured

  @cccp-education-real-migration
  Scenario: Migration réelle de la fixture cccp-education génère les messages et modifie les templates
    Given a real site directory "cccp-education-fixture" copied from test resources "i18n-fixtures/cccp-education/jbake"
    And i18n migration DSL configured with siteDir pointing to the real site and languages "fr,en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "Clés extraites"
    And the output should contain "Fichiers générés"
    And messages_fr.properties should exist in the templates directory
    And messages_en.properties should exist in the templates directory
    And the templates should contain th:text message keys
    And site.language=fr should be injected in jbake.properties

  @cccp-education-dry-run
  Scenario: Dry run sur la fixture cccp-education ne modifie rien
    Given a real site directory "cccp-education-fixture-dryrun" copied from test resources "i18n-fixtures/cccp-education/jbake"
    And i18n migration DSL configured with siteDir pointing to the real site and languages "fr,en" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "DRY-RUN"
    And no messages_en.properties file should exist in the templates directory
    And the templates should NOT be modified

  @cheroliv-com-real-migration
  Scenario: Migration réelle de la fixture cheroliv-com génère les messages et modifie les templates
    Given a real site directory "cheroliv-com-fixture" copied from test resources "i18n-fixtures/cheroliv-com/jbake"
    And i18n migration DSL configured with siteDir pointing to the real site and languages "fr,en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "Clés extraites"
    And the output should contain "Fichiers générés"
    And messages_fr.properties should exist in the templates directory
    And messages_en.properties should exist in the templates directory
    And the templates should contain th:text message keys
    And site.language=fr should be injected in jbake.properties

  @cheroliv-com-dry-run
  Scenario: Dry run sur la fixture cheroliv-com ne modifie rien
    Given a real site directory "cheroliv-com-fixture-dryrun" copied from test resources "i18n-fixtures/cheroliv-com/jbake"
    And i18n migration DSL configured with siteDir pointing to the real site and languages "fr,en" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And the output should contain "DRY-RUN"
    And no messages_en.properties file should exist in the templates directory
    And the templates should NOT be modified

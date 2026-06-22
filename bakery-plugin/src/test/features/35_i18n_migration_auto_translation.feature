@cucumber @bakery @i18n @migration @auto-translation
Feature: Migration i18n — Auto-traduction FR→EN avec fallback (BKY-I18N-MIG US-4)

  Quand la tâche `migrateToI18n` est configurée avec `languages="fr,en"`
  mais qu'aucun service de traduction LLM n'est injecté (IA désactivée ou
  LLM non disponible), les messages_en.properties restent vides après
  migration. Le remplissage automatique via le `TranslationService`
  transverse de codebase-gradle est testé au niveau unitaire et
  d'intégration avec un fake déterministe.

  Background:
    Given a new Bakery project with site fully configured

  @auto-translation-empty
  Scenario: Migration génère messages_en.properties vide quand aucun traducteur LLM n'est injecté
    Given a real site directory "auto-llm-site" with templates containing hardcoded French text
    And i18n migration DSL configured with siteDir pointing to the real site and languages "fr,en" and defaultLanguage "fr" and dryRun false
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And messages_en.properties should exist in the templates directory
    And messages_en.properties should have empty values
    And messages_fr.properties should exist in the templates directory
    And the templates should contain th:text message keys

  @auto-translation-dry-run
  Scenario: Dry-run preview does not write files
    Given a real site directory "auto-llm-dryrun" with templates containing hardcoded French text
    And i18n migration DSL configured with siteDir pointing to the real site and languages "fr,en" and defaultLanguage "fr" and dryRun true
    When I am executing the task 'migrateToI18n'
    Then the build should succeed
    And no messages_en.properties file should exist in the templates directory
    And the templates should NOT be modified

@cucumber @bakery @i18n @i18n-mass @validation
Feature: i18n mass validation — STRICT/LENIENT/OFF plantuml/table validation modes

  The `migrateContentI18n` task supports configurable validation modes for
  plantuml and table blocks during translation. The mode is set via DSL
  `bakery { contentI18nMigration { validation = "STRICT" } }` or CLI
  `--contentI18nValidation=STRICT`. A consolidated `validation-report.json`
  is generated post-passe.

  Background:
    Given a cheroliv-com-i18n-deploy fixture with 3 French articles

  Scenario: LENIENT mode generates validation report
    When the validation task migrates content from fr to "en" with validation mode "LENIENT"
    Then a validation report should exist at "build/i18n/validation-report.json"
    And the validation report should contain "table" and "plantUml" sections

  Scenario: STRICT mode generates validation report
    When the validation task migrates content from fr to "en" with validation mode "STRICT"
    Then a validation report should exist at "build/i18n/validation-report.json"
    And the validation report should contain "table" and "plantUml" sections

  Scenario: OFF mode generates validation report
    When the validation task migrates content from fr to "en" with validation mode "OFF"
    Then a validation report should exist at "build/i18n/validation-report.json"
    And the validation report should contain "table" and "plantUml" sections

  Scenario: Validation report is valid JSON
    When the validation task migrates content from fr to "en" with validation mode "LENIENT"
    Then the validation report should be valid JSON

  Scenario: Validation report is empty when no invalid blocks
    When the validation task migrates content from fr to "en" with validation mode "LENIENT"
    Then the validation report should have zero invalid entries

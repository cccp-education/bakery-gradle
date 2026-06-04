Feature: Verify Configuration Mapping

  US-61a — Couverture de test pour le mapping Jackson YAML ↔ Gradle properties sécurisé

  Scenario: Valid site.yml with secrets
    Given a valid site.yml with secrets
    When I run verifyConfigurationMapping
    Then the task succeeds and secrets are masked

  Scenario: Malformed YAML
    Given a malformed site.yml
    When I run verifyConfigurationMapping
    Then the task fails with parsing error

  Scenario: Missing configuration file
    Given a new Bakery project
    And the site.yml is missing
    When I run verifyConfigurationMapping
    Then the task fails with missing file error

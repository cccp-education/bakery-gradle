@bakery @ollama @i18n-ci
Feature: CI/CD Ollama Device Key end-to-end — buildMaskedSummary + validateRequiredFields (BKY-I18N-CI-6)

  `VerifyConfigurationMappingTask.buildMaskedSummary` logs the ollama device key
  count without leaking private keys. `validateRequiredFields` does not require
  ollama section (backward-compat). The `site.yml.example` template is the
  reference for the 29 Device Keys structure.

  Scenario: buildMaskedSummary includes ollama model, ports, and device key count
    Given a site.yml with 29 ollama device keys for ports 11437 to 11465
    When I build the masked summary
    Then the masked summary contains "ollama.model=gemma4:31b-cloud"
    And the masked summary contains "ollama.ports=11437-11465"
    And the masked summary contains "ollama.deviceKeys=29"
    And the masked summary does not contain "fake-key-11437"
    And the masked summary does not contain "fake-key-11465"

  Scenario: buildMaskedSummary without ollama section does not log ollama fields
    Given a site.yml without ollama section
    When I build the masked summary
    Then the masked summary does not contain "ollama.model"
    And the masked summary does not contain "ollama.ports"
    And the masked summary does not contain "ollama.deviceKeys"

  Scenario: validateRequiredFields passes without ollama section
    Given a site.yml without ollama section
    When I validate the required fields
    Then the validation passes without error

  Scenario: validateRequiredFields passes with ollama section
    Given a site.yml with 29 ollama device keys for ports 11437 to 11465
    When I validate the required fields
    Then the validation passes without error

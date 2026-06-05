@config-resolution @cs-fin-4
Feature: Config Resolution Cascade — ResolvedConfigs DRY (CS-FIN-4)

  As a bakery developer
  I want all 8 domain configs resolved in a single call via ResolvedConfigs
  So that the resolution logic is centralized and error-accumulating

  Scenario: All 8 configs resolve successfully with defaults
    Given a new Bakery project
    When I resolve all configs with defaults
    Then all 8 resolved configs should have default values
    And no resolution errors should be reported

  Scenario: CLI properties override all layers for all 8 configs
    Given a new Bakery project
    When I resolve all configs with CLI override "bakery.googleForms.formId=cli-form,bakery.analytics.provider=cli-matomo,bakery.theme.mode=dark"
    Then the resolved config should have googleForms.formId = "cli-form"
    And the resolved config should have analytics.provider = "cli-matomo"
    And the resolved config should have theme.mode = "dark"

  Scenario: ResolvedConfigs.toResolver injects all properties correctly
    Given a fully populated ResolvedConfigs
    When I convert it to a resolver function
    Then the resolver should map firebaseApiKey to the firebase api key
    And the resolver should map themeMode to the theme mode
    And the resolver should map layoutType to the layout type name
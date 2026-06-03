@cucumber @bakery @lens-n3
Feature: Augmented Context N3 Composite Context — BKY-LENS-5

  As a formateur using bakery
  I want the site context collector to enrich metadata.json with augmented entries from the N3 composite context
  So that runner-gradle can inject workspace-wide context into my bakery site

  Scenario: Composite context enriches metadata.json with augmented entries
    Given a minimal baked site directory
    And a composite-context.json file with 3 non-empty channels
    And the augmented context is enabled with contextPath set to the composite-context file
    When I collect site context with augmented context
    Then the metadata.json should contain 'augmentedEntries'
    And the augmentedEntries channels should have 3 entries

  Scenario: Composite context absent produces empty augmentedEntries
    Given a minimal baked site directory
    And the augmented context is enabled with contextPath set to a non-existent file
    When I collect site context with augmented context
    Then the metadata.json should contain 'augmentedEntries'
    And the augmentedEntries channels should be empty

  Scenario: Augmented context disabled does not add augmentedEntries
    Given a minimal baked site directory
    And the augmented context is disabled
    When I collect site context with augmented context
    Then the metadata.json should not contain 'augmentedEntries'
@bakery @tree
Feature: Scaffold IA returns a tree — BKY-TREE-7

  The `ScaffoldGenerator` parses a hierarchical `tree` (Site→Section→Article)
  when the LLM returns it, deriving `templates` via flatten. Backward compat:
  when `tree` is absent, the legacy `templates` list is accepted unchanged.
  The LLM prompt now requests a hierarchical JSON structure with a `type`
  discriminator per node.

  Scenario: Scaffold IA returns a valid tree with 3 levels
    Given an LLM response with a 3-level tree JSON
    When I generate the scaffold output
    Then the output tree is a Site with 2 sections
    And the output templates are derived from the tree leaves

  Scenario: Adapter flatten produces one template per article
    Given an LLM response with a tree containing 3 articles
    When I generate the scaffold output
    Then the output templates contains 3 entries
    And each template ends with ".thyme"

  Scenario: Backward compat — scaffold without tree uses legacy templates list
    Given an LLM response with a legacy templates list and no tree
    When I generate the scaffold output
    Then the output tree is null
    And the output templates equals the legacy list

  Scenario: Scaffold prompt requests hierarchical tree structure
    Given a scaffold intention for a formation site
    When I build the prompt
    Then the prompt contains "tree"
    And the prompt contains "section"
    And the prompt contains "article"
    And the prompt contains a type discriminator example

  Scenario: Integrity — tree present and templates present, tree wins
    Given an LLM response with both tree and legacy templates
    When I generate the scaffold output
    Then the output templates are derived from the tree
    And the output templates do not contain legacy entries
@bakery @tree
Feature: Site tree — Theme inheritance (BKY-TREE-5)

  The `ThemeResolver` resolves the effective theme per node by walking the
  ancestor chain (node → parent → root). The first explicit override found
  wins; if none, the default theme applies. An override is a total replacement
  — no merging with inherited values. Overrides on unknown paths are rejected.

  Scenario: Root node without override resolves to default theme
    Given a site tree with sections "formations" and "blog"
    And a default theme with primaryColor "#abc"
    When I resolve the theme of the root node
    Then the resolved primaryColor is "#abc"
    And the theme was resolved at path ""

  Scenario: Section inherits site default when no override
    Given a site tree with sections "formations" and "blog"
    And a default theme with primaryColor "#abc"
    When I resolve the theme of node "formations"
    Then the resolved primaryColor is "#abc"
    And the theme was resolved at path ""

  Scenario: Section override replaces inherited theme completely
    Given a site tree with sections "formations" and "blog"
    And a default theme with primaryColor "#abc" and mode "auto"
    And an override at "formations" with primaryColor "#f00" and mode "dark"
    When I resolve the theme of node "formations"
    Then the resolved primaryColor is "#f00"
    And the resolved mode is "dark"
    And the theme was resolved at path "formations"

  Scenario: Article under overridden section inherits section override
    Given a site tree with sections "formations" and "blog"
    And a default theme with primaryColor "#abc" and mode "auto"
    And an override at "formations" with primaryColor "#f00" and mode "dark"
    When I resolve the theme of node "formations/ab-partition"
    Then the resolved primaryColor is "#f00"
    And the resolved mode is "dark"
    And the theme was resolved at path "formations"

  Scenario: Article own override takes precedence over section override
    Given a site tree with sections "formations" and "blog"
    And a default theme with primaryColor "#abc" and mode "auto"
    And an override at "formations" with primaryColor "#f00" and mode "dark"
    And an override at "formations/ab-partition" with primaryColor "#00f" and mode "light"
    When I resolve the theme of node "formations/ab-partition"
    Then the resolved primaryColor is "#00f"
    And the resolved mode is "light"
    And the theme was resolved at path "formations/ab-partition"

  Scenario: Override does not merge with inherited — replacement is total
    Given a site tree with sections "formations" and "blog"
    And a default theme with primaryColor "#abc" and mode "dark" and fontFamily "Inter"
    And an override at "formations" with primaryColor "#f00"
    When I resolve the theme of node "formations"
    Then the resolved primaryColor is "#f00"
    And the resolved mode is "auto"
    And the resolved fontFamily is ""

  Scenario: Orphan override path not in tree is rejected
    Given a site tree with sections "formations" and "blog"
    And an override at "unknown/path" with primaryColor "#f00"
    When I create the theme resolver
    Then the creation fails

  Scenario: Root without default uses ThemeConfig defaults
    Given a site tree with sections "formations" and "blog"
    When I resolve the theme of the root node
    Then the resolved primaryColor is "#0d6efd"
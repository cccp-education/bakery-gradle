@bakery @tree
Feature: Site tree — SiteNode sealed interface (BKY-TREE-1)

  The domain models the editorial hierarchy of a static site via a Composite
  `SiteNode` with 3 implementations: `Site` (root), `Section` (intermediate
  node), `Article` (leaf). Identity is the canonical relative path. No I/O —
  pure domain classes.

  Scenario: An empty site has an empty root path and no sections
    Given an empty site
    Then the site path is empty
    And the site has 0 sections
    And the site is a section node
    And the site is not a leaf

  Scenario: A section containing an article forms a 3-level tree
    Given a section "formations" with an article "formations/ab-partition"
    When I build a site with this section
    Then the site has 1 section
    And the section "formations" has 1 article
    And the article "formations/ab-partition" is a leaf

  Scenario: An article is a leaf, not a section
    Given an article "formations/ab-partition"
    Then the node is a leaf
    And the node is not a section

  Scenario: Two articles with the same path are structurally equal
    Given two articles with path "formations/ab-partition"
    Then the two articles are equal

  Scenario: Two sections with the same path but different children are not equal
    Given a section "formations" with an article "formations/ab-partition"
    And an empty section "formations"
    Then the two sections are not equal

  Scenario: An article rejects an empty path
    When I create an article with an empty path
    Then the creation fails with a blank path error

  Scenario: An article path is canonical (no leading or trailing slash)
    Given an article "formations/ab-partition"
    Then the path "formations/ab-partition" is canonical
@bakery @tree
Feature: Site tree — traversal and visitors (BKY-TREE-2)

  `SiteTree` is the aggregate that wraps the `Site` root and exposes traversal
  operations: `walk` (pre/post-order), `filter`, `leaves`, `sections`,
  `findByPath`, `findSubtree`, `visit`. No in-place mutation — methods return
  new lists or trees.

  Scenario: Pre-order walk visits root then descendants depth-first
    Given a tree with two sections "formations" and "blog" and two articles
    When I walk the tree in pre-order
    Then the path "formations/ab-partition" comes before "formations/cd-partition" before "blog"

  Scenario: Post-order walk visits children before parents
    Given a tree with two sections "formations" and "blog" and two articles
    When I walk the tree in post-order
    Then the path "" is visited last

  Scenario: Leaves are only articles
    Given a tree with two sections "formations" and "blog" and two articles
    When I collect the leaves
    Then there are 2 leaves
    And the leaves have paths "formations/ab-partition" and "formations/cd-partition"

  Scenario: Sections include the root and intermediate sections
    Given a tree with two sections "formations" and "blog" and two articles
    When I collect the sections
    Then there are 3 sections
    And the sections have paths "" "formations" and "blog"

  Scenario: findByPath locates an article by its canonical path
    Given a tree with two sections "formations" and "blog" and two articles
    When I search for node "formations/ab-partition"
    Then the found node is a leaf

  Scenario: findByPath returns null for an unknown path
    Given a tree with two sections "formations" and "blog" and two articles
    When I search for node "unknown"
    Then no node is found

  Scenario: findSubtree returns a subtree rooted at the matching section
    Given a tree with two sections "formations" and "blog" and two articles
    When I search for subtree "formations"
    Then the subtree root is a section
    And the subtree contains 2 leaves

  Scenario: filter by type returns only articles
    Given a tree with two sections "formations" and "blog" and two articles
    When I filter nodes of type article
    Then there are 2 filtered nodes
    And all filtered nodes are leaves

  Scenario: visit applies a transform to every node
    Given a tree with two sections "formations" and "blog" and two articles
    When I visit the tree labelling each node by its type
    Then I get 5 labels
    And the first label is "site"
    And the last label is "section"
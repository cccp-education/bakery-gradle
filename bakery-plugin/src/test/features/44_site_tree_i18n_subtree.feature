@bakery @tree
Feature: Site tree — i18n migration per subtree (BKY-TREE-6)

  The `SubtreeI18nPlanner` replaces the flat `templates/` walk with a tree-aware
  delta computation. Only articles whose source checksum changed are flagged for
  re-translation. Articles outside the scoped subtree are untouched. Re-running
  the planner with the updated checksums yields an empty delta (Loi de l'Économie
  d'Encre par feuille). Backward compat: a flat article list is accepted when no
  tree is provided.

  Scenario: Delta is empty when no source changed
    Given a site tree with sections "formations" and "blog"
    And before checksums for all articles
    When I compute the delta with the same checksums
    Then the delta has 0 modified articles
    And the delta has 3 untouched articles

  Scenario: Delta detects a single modified article in the whole tree
    Given a site tree with sections "formations" and "blog"
    And before checksums for all articles
    When I compute the delta with "formations/ab-partition" changed
    Then the delta has 1 modified article
    And the modified article is "formations/ab-partition"

  Scenario: Subtree delta only includes modified articles within that subtree
    Given a site tree with sections "formations" and "blog"
    And before checksums for all articles
    When I compute the delta for subtree "formations" with "formations/ab-partition" and "blog/hello" changed
    Then the delta has 1 modified article
    And the modified article is "formations/ab-partition"

  Scenario: Integrity outside subtree is preserved
    Given a site tree with sections "formations" and "blog"
    And before checksums for all articles
    When I compute the delta for subtree "formations" with "blog/hello" changed
    Then the delta has 0 modified articles

  Scenario: Idempotence — re-execution with updated checksums yields empty delta
    Given a site tree with sections "formations" and "blog"
    And before checksums for all articles
    When I compute the delta with "formations/ab-partition" changed
    And I recompute the delta with the updated checksums
    Then the delta has 0 modified articles

  Scenario: Backward compat — flat article list is accepted without a tree
    Given a flat article list with 3 articles
    And before checksums for all articles
    When I compute the delta with "blog/hello" changed
    Then the delta has 1 modified article
    And the modified article is "blog/hello"
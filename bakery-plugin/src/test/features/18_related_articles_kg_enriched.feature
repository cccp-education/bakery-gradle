@cucumber @bakery @bkg-kg-enriched
Feature: Related Articles KG Enriched — BKY-BKG-0 co-occurrence + entity overlap

  As a formateur using bakery
  I want the knowledge graph to use tag co-occurrence and description entity overlap
  So that articles are connected by semantic similarity, not just shared tags

  Scenario: Tag co-occurrence creates stronger edges between frequently co-occurring tags
    Given 3 articles with overlapping tag pairs
    When the knowledge graph is built
    Then edge AB should have a cooccurrence reason
    And edge AB score should be greater than edge BC score
    And A and C should have no cooccurrence reason

  Scenario: Description entity overlap creates edges between semantically related articles
    Given 2 articles sharing significant entities in their descriptions
    When the knowledge graph is built
    Then the edge should have entity reasons
    And edge score should be greater than zero even without shared tags

  Scenario: Combined scoring with tags co-occurrence title keywords and entities
    Given 3 articles with shared tags overlapping descriptions and similar titles
    When the knowledge graph is built
    Then the edge with all factors combined should have the highest score
    And suggestions should order by decreasing relevance score
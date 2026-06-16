@cucumber @bakery @lens-augmented-context
Feature: Augmented Context Lens — BKY-LENS-2

  As a formateur using bakery
  I want the augmented context lens to score related articles using hybrid signals
  So that my readers see the most relevant content based on RAG, KG, tags, and cross-references

  Scenario: Scoring hybride combines RAG, graph proximity, tags, and cross-references
    Given a subgraph with 6 nodes and 4 communities
    And a RAG result with similarity 0.85 for node "article-a.md"
    When I score node "article-a.md" with current page tags "kotlin, gradle"
    Then the scored node should have ragSimilarity 0.85
    And the scored node should have graphProximity greater than 0.0
    And the scored node should have tagOverlap greater than 0.0
    And the scored node should have crossRefCount at least 0
    And the final score should be greater than 0.0

  Scenario: Filtrage des règles excludeTags
    Given a list of scored nodes with tags "draft", "wip", and "published"
    And lens rules with excludeTags containing "wip, draft"
    When I apply lens rules to the scored nodes
    Then the result should not contain any node with tag "draft"
    And the result should not contain any node with tag "wip"
    And the result should contain exactly 1 node with tag "published"

  Scenario: Budget maxArticles et minSimilarity tronquent les résultats
    Given a scored node list with 5 nodes and scores 0.9, 0.7, 0.6, 0.3, 0.1
    And a maxArticles budget of 3
    And a minSimilarity threshold of 0.5
    When I apply budget filtering
    Then the result should have exactly 3 nodes
    And all nodes should have score greater than or equal to 0.5
    And the result should be ordered by score descending

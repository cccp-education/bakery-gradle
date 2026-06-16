@cucumber @bakery @lens-hybrid-scoring
Feature: Lens Hybrid Scoring — BKY-LENS-2

  As a formateur using bakery
  I want the lens to score articles using hybrid signals from RAG, knowledge graph, tags, and cross-references
  So that my readers see the most semantically relevant content, not just chronological neighbors

  Scenario: Scoring combines RAG similarity, graph proximity, tag overlap, and cross-reference count
    Given a subgraph with 6 nodes and 4 communities
    And a RAG result with similarity 0.90 for node "article-a.md"
    When I score node "article-a.md" with current page tags "kotlin, gradle, plugin"
    Then the scored node should have ragSimilarity 0.90
    And the scored node should have graphProximity greater than 0.0
    And the scored node should have tagOverlap greater than 0.0
    And the final score should be greater than 0.3

  Scenario: Lens rules exclude drafts and WIP articles from suggestions
    Given a list of scored nodes with tags "draft", "wip", and "published"
    And lens rules with excludeTags containing "wip, draft"
    When I apply lens rules to the scored nodes
    Then the result should not contain any node with tag "draft"
    And the result should not contain any node with tag "wip"
    And the result should contain exactly 1 node with tag "published"

  Scenario: Budget truncates results by maxArticles and minSimilarity threshold
    Given a scored node list with 5 nodes and scores 0.95, 0.80, 0.75, 0.40, 0.10
    And a maxArticles budget of 2
    And a minSimilarity threshold of 0.7
    When I apply budget filtering
    Then the result should have exactly 2 nodes
    And all nodes should have score greater than or equal to 0.7
    And the result should be ordered by score descending
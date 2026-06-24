@bakery @tree
Feature: Site tree — YAML serialization of tree: section in site.yml (BKY-TREE-4)

  `SiteTreeYaml` serializes and parses a `SiteNode` to YAML via Jackson
  polymorphic (intermediate DTO with `@JsonTypeInfo` + `@JsonSubTypes`).
  Round-trip parse/serialize is deterministic. The `tree:` section is
  additive — a `site.yml` without `tree:` remains valid (fallback flat
  behavior). The `type` discriminator distinguishes site/section/article.
  `content` (AST) is not serialized — it is `@JsonIgnore`.

  Scenario: Round-trip of an empty tree preserves structure
    Given an empty tree
    When I serialize and re-parse the tree
    Then the reparsed tree is identical to the original

  Scenario: Round-trip of a 3-level tree preserves structure
    Given a 3-level tree with 2 articles
    When I serialize and re-parse the tree
    Then the reparsed tree is identical to the original

  Scenario: Serialized YAML contains the type discriminator
    Given a 3-level tree with 2 articles
    When I serialize the tree
    Then the YAML contains "type: site"
    And the YAML contains "type: section"
    And the YAML contains "type: article"

  Scenario: Parsing an empty YAML returns null (compatibility)
    When I parse an empty YAML
    Then the result is null

  Scenario: Parsing an invalid YAML returns null via parseOrNull
    When I parse a YAML with an unknown type
    Then the parseOrNull result is null

  Scenario: A serialized article does not contain its AST content
    Given an article "s/a" with content
    When I serialize the article
    Then the YAML does not contain "content"
    And the YAML does not contain "pivot"
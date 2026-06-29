@bakery @tree @tree-bake
Feature: Tree config consumption in bake pipeline (BKY-OUTPUT-4)

  `SiteConfiguration.tree` is populated from the `tree:` YAML section in
  `site.yml`. The tree config is resolved (ancestor inheritance) and
  injected into `jbake.properties` as `bakeTreeConfig` JSON for Thymeleaf
  templates. A site.yml without `tree:` remains valid.

  Scenario: A site.yml without tree section has null tree
    Given a basic site.yml
    When I parse the site configuration
    Then the tree field is null

  Scenario: A site.yml with tree section populates the tree field
    Given a site.yml with a tree section
    When I parse the site configuration
    Then the tree field is present
    And the tree has 1 section
    And the parsed section "blog" has 1 article

  Scenario: The tree section is resolved in jbake properties injection
    Given a site.yml with tree section and output config
    When I run the bake pipeline
    Then the jbake properties contain "bakeTreeConfig"
    And the baked site is generated

  Scenario: Article inherits output config from parent section
    Given a site.yml with section-level layout
    And an article without explicit layout
    When I run the bake pipeline
    Then the article's resolved layout is "SIDEBAR_LEFT"

  Scenario: Article override wins over section config
    Given a site.yml with a section that has layout "SIDEBAR_LEFT"
    And an article with layout "CENTERED"
    When I run the bake pipeline
    Then the article's resolved layout is "CENTERED"

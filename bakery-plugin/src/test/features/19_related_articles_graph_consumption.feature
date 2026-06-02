@cucumber @bakery @bkg-graph-consumption
Feature: Related Articles Graph Consumption — BKY-BKG-1

  As a formateur using bakery
  I want bakery to consume the related-articles.json graph at bake time
  So that Thymeleaf templates can display article descriptions and tags from the knowledge graph

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Related-articles config with graphFilePath injects path into jbake properties
    And the site configuration contains a relatedArticles block with enabled=true and graphFilePath 'custom/graph.json'
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'relatedArticlesGraphFilePath=custom/graph.json'
    And the file 'site/jbake.properties' should contain 'relatedArticlesEnabled=true'

  @default-path
  Scenario: Related-articles config without graphFilePath uses default build/bakery path
    And the site configuration contains a relatedArticles block with enabled=true
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'relatedArticlesGraphFilePath=build/bakery/related-articles.json'
    And the file 'site/jbake.properties' should contain 'relatedArticlesEnabled=true'

  @output-enriched
  Scenario: RelatedArticlesOutput contains blogArticles with description and tags
    Given 4 articles with overlapping tags for graph consumption
    When the knowledge graph is built and suggestions generated
    Then the output should contain blogArticles for all 4 articles
    And each blogArticle should have a description matching the input

  @bkg-1.4-template
  Scenario: Related-articles template renders data-related-articles attribute with graph JSON
    And the site configuration contains a relatedArticles block with enabled=true and graphFilePath 'build/bakery/related-articles.json'
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'relatedArticlesEnabled=true'
    And the file 'site/jbake.properties' should contain 'relatedArticlesData='

  @bkg-1.4-empty-data
  Scenario: Related-articles template renders with empty data when graph file does not exist
    And the site configuration contains a relatedArticles block with enabled=true and graphFilePath 'nonexistent/path.json'
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'relatedArticlesEnabled=true'
    And the file 'site/jbake.properties' should contain 'relatedArticlesData='
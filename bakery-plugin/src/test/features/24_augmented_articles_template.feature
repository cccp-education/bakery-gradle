@cucumber @bakery @lens-augmented-templates
Feature: Augmented Articles Template — BKY-LENS-6

  As a formateur using bakery
  I want the augmented-articles fragment to display related workspace articles with source badges (RAG/KG/Docs/EAGER)
  So that my site visitors can discover contextually relevant content with transparency about sources

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Augmented-articles template is scaffolded with blog site
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the project should have a file named 'augmented-articles.thyme' in the site templates directory

  @content
  Scenario: Augmented-articles template has th-if guard on augmentedContextEnabled
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/augmented-articles.thyme' should contain 'th:if'
    And the file 'site/templates/augmented-articles.thyme' should contain 'augmentedContextEnabled'

  @content
  Scenario: Augmented-articles template contains badge rendering JavaScript
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/augmented-articles.thyme' should contain 'scoredNodes'
    And the file 'site/templates/augmented-articles.thyme' should contain 'data-channel'
    And the file 'site/templates/augmented-articles.thyme' should contain 'badge-source-'

  @integration
  Scenario: Post template references augmented-articles fragment
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/post.thyme' should contain 'augmented-articles'
    And the file 'site/templates/post.thyme' should not contain 'related-articles.thyme::related-articles'

  @integration
  Scenario: Page template references augmented-articles fragment
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/page.thyme' should contain 'augmented-articles'
@cucumber @bakery @bkg-related-articles
Feature: Related Articles KG — BKY-BKG

  As a formateur using bakery
  I want to display knowledge-graph related articles in my blog posts
  So that my readers discover semantically relevant content instead of just chronological neighbors

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Blog site scaffolding includes related-articles template
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the project should have a file named 'related-articles.thyme' in the site templates directory
    And the file 'site/templates/related-articles.thyme' should contain 'relatedArticlesEnabled'
    And the file 'site/templates/related-articles.thyme' should contain 'th:if'
    And the file 'site/templates/related-articles.thyme' should contain 'related-articles-list'

  @no-config
  Scenario: Blog site without relatedArticles config injects defaults in jbake properties
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'relatedArticlesEnabled=false'
    And the file 'site/jbake.properties' should contain 'relatedArticlesMaxResults=4'

  @integration
  Scenario: Post template includes related-articles fragment
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/post.thyme' should contain 'related-articles.thyme::related-articles'
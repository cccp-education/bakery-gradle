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

  # ─── Thymeleaf Rendering (LENS-6.3) ──────────────────────────────────

  @rendering @lens-augmented-templates
  Scenario: Augmented-articles — Rendered HTML shows section with data-attributes when context enabled
    Given the template context has "augmentedContextEnabled" = "true"
    And the template context has "augmentedContextData" = '{"scoredNodes":[{"uri":"/post1","title":"Post 1","channels":["RAG","KG"]},{"uri":"/post2","title":"Post 2","channels":["Docs"]}]}'
    And the template context has "lensBudgetMaxArticlesPerPage" = "4"
    When I render the template "augmented-articles"
    Then the rendered HTML should contain "augmented-articles"
    And the rendered HTML should contain "data-augmented-context"
    And the rendered HTML should contain "scoredNodes"
    And the rendered HTML should not contain "th:if"

  @rendering @lens-augmented-templates
  Scenario: Augmented-articles — No section when context not enabled
    When I render the template "augmented-articles"
    Then the rendered HTML should not contain "augmented-articles"
    And the rendered HTML should not contain "data-augmented-context"
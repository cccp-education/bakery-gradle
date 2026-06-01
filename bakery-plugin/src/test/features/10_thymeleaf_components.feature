@cucumber @bakery @jb2-components
Feature: Reusable Thymeleaf components are scaffolded with blog site

  As a site creator using bakery
  I want reusable Thymeleaf components (breadcrumb, toc-sidebar, progress-bar, pdf-viewer)
  So that my blog pages have navigation aids and enhanced UX out of the box

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Blog site scaffolding includes all 4 Thymeleaf components
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the project should have a file named 'breadcrumb.thyme' in the site templates directory
    And the project should have a file named 'toc-sidebar.thyme' in the site templates directory
    And the project should have a file named 'progress-bar.thyme' in the site templates directory
    And the project should have a file named 'pdf-viewer.thyme' in the site templates directory

  @content
  Scenario: Breadcrumb component contains navigation markup
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/breadcrumb.thyme' should contain 'breadcrumb'
    And the file 'site/templates/breadcrumb.thyme' should contain 'Accueil'

  @content
  Scenario: TOC sidebar component contains table of contents markup
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/toc-sidebar.thyme' should contain 'toc-sidebar'
    And the file 'site/templates/toc-sidebar.thyme' should contain 'Sommaire'

  @content
  Scenario: Progress bar component contains reading progress markup
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/progress-bar.thyme' should contain 'progress-bar'
    And the file 'site/templates/progress-bar.thyme' should contain 'reading-progress-bar'

  @content
  Scenario: PDF viewer component contains embed markup
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/pdf-viewer.thyme' should contain 'pdf-viewer'
    And the file 'site/templates/pdf-viewer.thyme' should contain 'iframe'
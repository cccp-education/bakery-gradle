@cucumber @bakery @jb7-layouts
Feature: Layouts — BKY-JB-7

  As a bakery site developer
  I want to choose a page layout (full-width, sidebar, centered)
  So that my site content is displayed in the appropriate structure

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Blog site scaffolding includes all layout templates
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the project should have a file named 'layout-full-width.thyme' in the site templates directory
    And the project should have a file named 'layout-sidebar-left.thyme' in the site templates directory
    And the project should have a file named 'layout-sidebar-right.thyme' in the site templates directory
    And the project should have a file named 'layout-centered.thyme' in the site templates directory

  @default-layout
  Scenario: Default layout type is FULL_WIDTH in jbake properties
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'layoutType=FULL_WIDTH'

  @template-content
  Scenario: Layout templates have th:if guards for conditional rendering
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/layout-full-width.thyme' should contain 'th:if'
    And the file 'site/templates/layout-sidebar-left.thyme' should contain 'th:if'
    And the file 'site/templates/layout-sidebar-right.thyme' should contain 'th:if'
    And the file 'site/templates/layout-centered.thyme' should contain 'th:if'
@cucumber @bakery @jb6-theme-system
Feature: Theme System — BKY-JB-6

  As a formateur using bakery
  I want to customize my site theme (colors, font, logo, favicon, dark/light mode)
  So that my site reflects my brand identity

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Blog site scaffolding includes theme-script template
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the project should have a file named 'theme-script.thyme' in the site templates directory
    And the file 'site/templates/theme-script.thyme' should contain 'themePrimaryColor'
    And the file 'site/templates/theme-script.thyme' should contain 'th:if'
    And the file 'site/templates/theme-script.thyme' should contain '--bakery-primary'

  @no-config
  Scenario: Blog site without theme config has no injection in jbake properties
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'themeMode'
    And the file 'site/jbake.properties' should not contain 'themePrimaryColor'

  @integration
  Scenario: Header template includes theme-script and conditional favicon
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/header.thyme' should contain 'theme-script'
    And the file 'site/templates/header.thyme' should contain 'themeFaviconUrl'
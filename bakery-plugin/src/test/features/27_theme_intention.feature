@cucumber @bakery @ia2-theme-intention
Feature: Theme Intention — BKY-IA-2

  As a formateur using bakery
  I want to select a theme variant from the catalog and customize CSS variables
  So that my site reflects my brand identity through parametric theming

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Theme variant selection injects preset colors into jbake properties
    Given does not have 'site.yml' for site configuration
    And the bakery DSL defines theme.variant as "magazine"
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'themeVariant=magazine'
    And the file 'site/jbake.properties' should contain 'themePrimaryColor='
    And the file 'site/jbake.properties' should contain 'themeAccentColor='

  @catalog-override
  Scenario: Theme variant with DSL accent color override
    Given does not have 'site.yml' for site configuration
    And the bakery DSL defines theme.variant as "formation"
    And the bakery DSL defines theme.accentColor as "#FF5733"
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'themeAccentColor=#FF5733'
    And the file 'site/jbake.properties' should contain 'themeVariant=formation'

  @no-variant
  Scenario: Theme without variant injects defaults
    Given does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'themePrimaryColor=#0d6efd'
    And the file 'site/jbake.properties' should contain 'themeMode=auto'
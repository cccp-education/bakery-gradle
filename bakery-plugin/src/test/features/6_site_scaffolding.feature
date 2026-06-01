#noinspection CucumberUndefinedStep
@cucumber @bakery @scaffolding
Feature: Scaffold a new static site into the sites base directory

  Background:
    Given a new Bakery project

  # ================================================================
  # UJ1 — Scaffolder un nouveau site avec sitesBaseDir + siteName
  # ================================================================
  @happy-path
  Scenario: Scaffold a new site into the sites base directory
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'
    And does not have 'site.yml' for site configuration
    And the gradle project does not have 'site' directory for site
    And the gradle project does not have 'index.html' file for maquette
    And the bakery extension defines 'sitesBaseDir' as 'office/sites'
    And the bakery extension defines 'siteName' as 'my-company'
    When I am executing the task 'generateSite'
    Then the project directory 'office/sites/my-company' should exist
    Then the directory 'office/sites/my-company' should have a 'site.yml' file for site configuration
    Then the directory 'office/sites/my-company' should have a directory named 'site' who contains 'jbake.properties' file
    Then the directory 'office/sites/my-company' should have a directory named 'maquette' who contains 'index.html' file
    Then the directory 'office/sites/my-company' should have a file named '.gitignore' who contains 'site.yml', '.gradle', 'build' and '.kotlin'
    Then the directory 'office/sites/my-company' should have a file named '.gitattributes' who contains 'eol' and 'crlf'

  # ================================================================
  # UJ2 — Scaffolding sans siteName, avec sitesBaseDir → erreur
  # ================================================================
  @error-handling
  Scenario: Scaffold fails when siteName is missing but sitesBaseDir is defined
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'
    And the bakery extension defines 'sitesBaseDir' as 'office/sites'
    And the bakery extension does not define 'siteName'
    When I am executing the task 'generateSite' expecting failure
    Then the build output should contain 'siteName must be defined'

  # ================================================================
  # UJ3 — Scaffolder un site qui existe déjà → erreur
  # ================================================================
  @error-handling
  Scenario: Scaffold fails when the site directory already exists
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'
    And the bakery extension defines 'sitesBaseDir' as 'office/sites'
    And the bakery extension defines 'siteName' as 'existing-site'
    And the directory 'office/sites/existing-site' already exists
    When I am executing the task 'generateSite' expecting failure
    Then the build output should contain 'already exists'

  # =================================================================
  # UJ4 — sitesBaseDir absent, siteName présent → projectDir/<siteName>
  # =================================================================
  @happy-path
  Scenario: Scaffold into project directory when only siteName is defined
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'
    And does not have 'site.yml' for site configuration
    And the gradle project does not have 'site' directory for site
    And the gradle project does not have 'index.html' file for maquette
    And the bakery extension defines 'siteName' as 'mysite'
    And the bakery extension does not define 'sitesBaseDir'
    When I am executing the task 'generateSite'
    Then the project directory 'mysite' should exist
    Then the directory 'mysite' should have a 'site.yml' file for site configuration
    Then the directory 'mysite' should have a directory named 'site' who contains 'jbake.properties' file
    Then the directory 'mysite' should have a directory named 'maquette' who contains 'index.html' file

  # =================================================================
  # UJ5 — ni sitesBaseDir ni siteName → rétrocompatibilité
  #   generateSite crée tout dans projectDir comme aujourd'hui
  # =================================================================
  @backward-compat
  Scenario: Scaffold into project root when no scaffolding properties are defined (backward compat)
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'
    And does not have 'site.yml' for site configuration
    And the gradle project does not have 'site' directory for site
    And the gradle project does not have 'index.html' file for maquette
    When I am executing the task 'generateSite'
    Then the project should have a 'site.yml' file for site configuration
    Then the project should have a directory named 'site' who contains 'jbake.properties' file
    Then the project should have a directory named 'maquette' who contains 'index.html' file
    Then the project should have a file named '.gitignore' who contains 'site.yml', '.gradle', 'build' and '.kotlin'
    Then the project should have a file named '.gitattributes' who contains 'eol' and 'crlf'

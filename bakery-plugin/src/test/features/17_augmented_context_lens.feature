@cucumber @bakery @lens-augmented-context
Feature: Augmented Context Lens — BKY-LENS-4

  As a formateur using bakery
  I want the augmented context lens to inject configuration into JBake properties
  So that my JBake templates can render related workspace content based on the LENS pipeline

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Augmented context enabled injects LENS keys into jbake properties
    And does not have 'site.yml' for site configuration
    And the bakery DSL defines augmentedContext as enabled with maxArticlesPerPage 4 and minSimilarity 0.7
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'augmentedContextEnabled=true'
    And the file 'site/jbake.properties' should contain 'lensBudgetMaxArticlesPerPage=4'
    And the file 'site/jbake.properties' should contain 'lensBudgetMinSimilarity=0.7'

  @budget-custom
  Scenario: Custom budget values are injected into jbake properties
    And does not have 'site.yml' for site configuration
    And the bakery DSL defines augmentedContext as enabled with maxArticlesPerPage 6 and minSimilarity 0.8
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should contain 'lensBudgetMaxArticlesPerPage=6'
    And the file 'site/jbake.properties' should contain 'lensBudgetMinSimilarity=0.8'

  @disabled
  Scenario: Augmented context disabled does not inject LENS keys
    And does not have 'site.yml' for site configuration
    And the bakery DSL defines augmentedContext as disabled
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'augmentedContextEnabled=true'
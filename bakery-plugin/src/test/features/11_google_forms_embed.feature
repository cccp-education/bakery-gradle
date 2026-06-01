@cucumber @bakery @jb3-google-forms
Feature: Google Forms Embed — BKY-JB-3

  As a formateur using bakery
  I want to embed a Google Form in a JBake page
  So that I can collect evaluations or registrations directly on my site

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Blog site scaffolding includes google-forms template
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the project should have a file named 'google-forms.thyme' in the site templates directory
    And the file 'site/templates/google-forms.thyme' should contain 'google-forms-container'
    And the file 'site/templates/google-forms.thyme' should contain 'iframe'
    And the file 'site/templates/google-forms.thyme' should contain 'googleFormsFormId'

  @no-config
  Scenario: Blog site without googleForms config has no injection in jbake properties
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'googleForms'

  @template-content
  Scenario: Google forms template has th:if guard to hide iframe when formId is absent
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/google-forms.thyme' should contain 'th:if'
    And the file 'site/templates/google-forms.thyme' should contain 'docs.google.com/forms'
@cucumber @bakery @jb5-analytics-newsletter
Feature: Analytics + Newsletter — BKY-JB-5

  As a formateur using bakery
  I want analytics tracking and a newsletter subscription form on my site
  So that I can measure traffic and grow my mailing list

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Blog site scaffolding includes analytics-script and newsletter-form templates
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the project should have a file named 'analytics-script.thyme' in the site templates directory
    And the project should have a file named 'newsletter-form.thyme' in the site templates directory
    And the file 'site/templates/analytics-script.thyme' should contain 'analyticsProvider'
    And the file 'site/templates/analytics-script.thyme' should contain 'th:if'
    And the file 'site/templates/newsletter-form.thyme' should contain 'newsletterEnabled'
    And the file 'site/templates/newsletter-form.thyme' should contain 'th:if'

  @no-config
  Scenario: Blog site without analytics and newsletter config has no injection in jbake properties
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'analyticsProvider'
    And the file 'site/jbake.properties' should not contain 'newsletterEnabled'

  @integration
  Scenario: Footer template includes analytics-script and newsletter-form fragments
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/footer.thyme' should contain 'analytics-script'
    And the file 'site/templates/footer.thyme' should contain 'newsletter-form'
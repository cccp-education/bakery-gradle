@cucumber @bakery @conv-1
Feature: Surcharge Configuration Multi-Couche — BKY-CONV-1

  As a developer using bakery
  I want each configuration property to be overridable by the upper layer
  So that I can customize the build without modifying site.yml

  Background:
    Given a new Bakery project
    And does not have 'site.yml' for site configuration

  @defaults-fallback
  Scenario: Defaults are injected when no config provided
    When I am executing the task 'generateSite'
    Then the build should succeed

  @yaml-injection
  Scenario: Theme defaults are always injected in jbake.properties
    When I am executing the task 'generateSite'
    Then the build should succeed
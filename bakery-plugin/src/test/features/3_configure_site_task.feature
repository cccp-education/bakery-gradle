#noinspection CucumberUndefinedStep
@cucumber @bakery
Feature: The collect config task set the deployment configuration

  Scenario: collectSiteConfig task is registered with correct group and description
    Given a new Bakery project
    And the project has the bake and maquette directories ready
    When I am executing the task 'tasks'
    Then the build should succeed
    And the output should contain collectSiteConfig task information

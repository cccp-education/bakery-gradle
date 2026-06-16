#noinspection CucumberUndefinedStep
@cucumber @bakery @ia
Feature: IA configuration via DSL bakery { ia { ... } }

  Scenario: Default IA configuration compiles without error
    Given a new Bakery project with IA block
    When I am executing the task 'tasks'
    Then the build should succeed

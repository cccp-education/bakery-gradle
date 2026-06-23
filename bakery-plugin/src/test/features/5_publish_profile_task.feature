@cucumber @bakery
Feature: The deployProfile task pushes profile files to GitHub

  Scenario: deployProfile task is registered when pushProfile is present in site.yml
    Given an existing Bakery project with pushProfile configuration
    And the output of the task 'tasks' contains 'deployProfile' from the group 'deploy' and 'Push profile files (e.g. README.md) to GitHub repository'

  Scenario: deployProfile task is always registered regardless of pushProfile configuration
    Given an existing empty Bakery project using DSL with 'site.yml' file
    And the output of the task 'tasks' contains 'deployProfile' from the group 'deploy' and 'Push profile files (e.g. README.md) to GitHub repository'

  @end-to-end
  Scenario: deployProfile pushes profile files to a simulated remote and preserves history
    Given an existing Bakery project with pushProfile pointing to a simulated remote
    And the simulated remote has a file "old.txt" with content "existing remote content"
    And the project has profile files:
      | README.md    |
      | README_fr.md |
    When I execute the deployProfile task with credentials "testuser" and "testtoken"
    Then the simulated remote should contain "README.md"
    And the simulated remote should contain "README_fr.md"
    And the simulated remote should still contain "old.txt" with content "existing remote content"

  Scenario: deployProfile fails when site.yml does not exist
    Given an existing Bakery project with pushProfile configuration
    And the project has profile files:
      | README.md |
    And the file "site.yml" is removed from the project
    When I execute the deployProfile task with credentials "testuser" and "testtoken"
    Then the deployProfile task should fail with message containing "Failed to read site.yml"

  Scenario: deployProfile fails when no credentials are provided
    Given an existing Bakery project with pushProfile pointing to a simulated remote
    And the project has profile files:
      | README.md |
    When I execute the deployProfile task with credentials "" and ""
    Then the deployProfile task should fail with message containing "credentials not found"

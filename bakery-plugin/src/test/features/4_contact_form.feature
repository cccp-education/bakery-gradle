#noinspection CucumberUndefinedStep
@cucumber @bakery
Feature: The site template contact form

  Scenario: `generateSite` injects Firebase config into jbake.properties
    Given an existing empty Bakery project using DSL with 'site.yml' file
    And the output of the task 'tasks' contains 'generateSite' from the group 'generate' and 'Initialise site and maquette folders.'
    When I am executing the task 'generateSite'
    Then the project should have a directory named 'site' who contains 'jbake.properties' file
    And the 'jbake.properties' file in 'site' directory should contain 'firebaseApiKey' and 'firebaseProjectId'

  Scenario: `generateSite` deploys contact form templates into site directory
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And does not have 'site.yml' for site configuration
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'
    When I am executing the task 'generateSite'
    Then the file 'templates/contact.thyme' in 'site' directory should exist
    And the file 'templates/contact.thyme' in 'site' directory should contain 'contact-section'
    And the file 'templates/footer.thyme' in 'site' directory should contain 'FIREBASE_CONFIG'
    And the file 'templates/index.thyme' in 'site' directory should contain 'contact.thyme::contact-section'
    And the file 'templates/menu.thyme' in 'site' directory should contain '#contact'

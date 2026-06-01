@cucumber @bakery @jb4-firebase-auth
Feature: Firebase Auth + Comments — BKY-JB-4

  As a formateur using bakery
  I want Firebase Authentication and Firestore Comments on my site
  So that students can log in and leave feedback on training pages

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  @happy-path
  Scenario: Blog site scaffolding includes auth-header and comments templates
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the project should have a file named 'auth-header.thyme' in the site templates directory
    And the project should have a file named 'comments.thyme' in the site templates directory
    And the file 'site/templates/auth-header.thyme' should contain 'firebaseAuthApiKey'
    And the file 'site/templates/auth-header.thyme' should contain 'th:if'
    And the file 'site/templates/comments.thyme' should contain 'commentsEnabled'
    And the file 'site/templates/comments.thyme' should contain 'th:if'

  @no-config
  Scenario: Blog site without firebaseAuth config has no injection in jbake properties
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'firebaseAuth'
    And the file 'site/jbake.properties' should not contain 'commentsEnabled'

  @integration
  Scenario: Menu and post templates include auth and comments fragments
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/templates/menu.thyme' should contain 'auth-header.thyme'
    And the file 'site/templates/post.thyme' should contain 'comments.thyme'
    And the file 'site/templates/footer.thyme' should contain 'firebase-auth-compat'
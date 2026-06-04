@cucumber @bakery @ia3-firebase-validation
Feature: Firebase Configuration Validation — BKY-IA-3

  As a formateur using bakery
  I want to validate my Firebase configuration before deploying
  So that I catch misconfigurations early and avoid runtime errors

  Background:
    Given a new Bakery project with site configured but without IA

  @happy-path
  Scenario: Valid Firebase Auth config validates successfully
    Given the firebaseAuth DSL is configured with apiKey "AIzaSyB-valid" and authDomain "my-project.firebaseapp.com" and projectId "my-project"
    When I am executing the task 'validateFirebaseConfig'
    Then the build should succeed
    And the output should contain "✅ Configuration Firebase valide"

  @validation-error
  Scenario: Invalid config produces errors
    Given the firebaseAuth DSL is configured with apiKey "" and authDomain "" and projectId ""
    When I am executing the task 'validateFirebaseConfig' expecting failure
    Then the output should contain "Configuration Firebase invalide"

  @ia-option
  Scenario: Without IA enabled, validation reports IA disabled
    Given the firebaseAuth DSL is configured with apiKey "AIzaSyB-test" and authDomain "test-project.firebaseapp.com" and projectId "test-project"
    When I am executing the task 'validateFirebaseConfig'
    Then the build should succeed
    And the output should contain "Validation IA désactivée"

  #noinspection CucumberUndefinedStep
  @phone-validation
  Scenario: Phone field with wrong type produces warning
    Given the firebaseAuth DSL is configured with apiKey "AIzaSyB-test" and authDomain "test-project.firebaseapp.com" and projectId "test-project"
    And the contact form firestore has a phone field with type "number"
    When I am executing the task 'validateFirebaseConfig'
    Then the build should succeed
    And the output should contain "phone field should be of type 'string'"

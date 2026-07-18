@cucumber @bakery @i18n @plantuml-deploy
Feature: PlantUML translation adapter applies strategy to plantuml blocks

  The PlantUmlTranslationAdapter applies the strategy decided by the
  PlantUmlClassifier to a `[plantuml]` source block during content
  translation. TranslateLabels translates quoted labels through the
  TranslationService port. PreserveTechnical leaves the block untouched.
  BorrowVocabulary translates labels while preserving borrowed business
  vocabulary (REAC, AFNOR, DC, TS) verbatim so the LLM never rewrites the
  term.

  Background:
    Given a plantuml translation adapter with a label-aware fake translator

  Scenario: Non-plantuml source block is returned unchanged
    Given a source block of language "kotlin" with content "val x = 1"
    When the plantuml adapter translates the block from fr to en
    Then the returned block content should be "val x = 1"

  Scenario: PreserveTechnical leaves a purely technical block intact
    Given a plantuml source block with only technical syntax
    When the plantuml adapter translates the block from fr to en
    Then the returned block content should equal the original block content

  Scenario: TranslateLabels translates quoted labels only
    Given a plantuml source block with translatable labels "Utilisateur" and "Service"
    When the plantuml adapter translates the block from fr to en
    Then the returned block content should contain "User"
    And the returned block content should not contain "Utilisateur"

  Scenario: TranslateLabels keeps the block structure (startuml, class, enduml)
    Given a plantuml source block with translatable labels "Utilisateur" and "Service"
    When the plantuml adapter translates the block from fr to en
    Then the returned block content should contain "@startuml"
    And the returned block content should contain "@enduml"

  Scenario: BorrowVocabulary preserves REAC and AFNOR verbatim
    Given a plantuml source block with borrowed vocabulary REAC and AFNOR and a translatable label "Référentiel"
    When the plantuml adapter translates the block from fr to en
    Then the returned block content should contain "REAC"
    And the returned block content should contain "AFNOR"

  Scenario: BorrowVocabulary still translates the non-vocabulary labels
    Given a plantuml source block with borrowed vocabulary REAC and a translatable label "Utilisateur"
    When the plantuml adapter translates the block from fr to en
    Then the returned block content should contain "REAC"
    And the returned block content should contain "User"
    And the returned block content should not contain "Utilisateur"

  Scenario: TranslateLabels skips identifiers that look like qualified code
    Given a plantuml source block with a code identifier "com.example.UserService" and a label "Service"
    When the plantuml adapter translates the block from fr to en
    Then the returned block content should contain "com.example.UserService"

  Scenario: TranslateLabels keeps the block language as plantuml
    Given a plantuml source block with translatable labels "Utilisateur" and "Service"
    When the plantuml adapter translates the block from fr to en
    Then the returned block language should be "plantuml"

  Scenario: TranslateLabels returns the original label when the translator fails
    Given a plantuml translation adapter with a failing translator
    And a plantuml source block with translatable labels "Utilisateur" and "Service"
    When the plantuml adapter translates the block from fr to en
    Then the returned block content should contain "Utilisateur"